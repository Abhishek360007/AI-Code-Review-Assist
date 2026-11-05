package com.reviewassist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reviewassist;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-secret-that-is-definitely-longer-than-32-chars",
        "app.ai.provider=demo",
        "app.review.max-code-chars=500",
})
class ReviewFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String register(String username) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "username", username, "email", username + "@example.com", "password", "correct-horse"));
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("token").asText();
    }

    @Test
    void reviewsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/reviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reviews").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metaIsPublic() throws Exception {
        mvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("demo-rules"))
                .andExpect(jsonPath("$.aiEnabled").value(false));
    }

    @Test
    void loginWorksWithUsernameOrEmail() throws Exception {
        register("dana");
        for (String login : new String[]{"dana", "DANA@example.com"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("login", login, "password", "correct-horse"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("dana"));
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", "dana", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        register("erin");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", "ERIN", "email", "other@example.com", "password", "correct-horse"))))
                .andExpect(status().isConflict());
    }

    @Test
    void fullReviewLifecycle() throws Exception {
        String token = register("alice");
        String code = """
                function login(user) {
                  var apiKey = "sk-1234567890";
                  if (user == null) { return; }
                  console.log(user);
                }
                """;

        String created = mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("language", "javascript", "code", code))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findings[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.findings[0].line").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).path("id").asLong();

        mvc.perform(get("/api/reviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].worstSeverity").value("CRITICAL"));

        JsonNode stats = json.readTree(mvc.perform(get("/api/reviews/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(stats.path("totalReviews").asLong()).isEqualTo(1);
        assertThat(stats.path("findingsBySeverity").path("CRITICAL").asLong()).isEqualTo(1);

        // Another user cannot see or delete it.
        String bobToken = register("bob");
        mvc.perform(get("/api/reviews/" + id).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/reviews/" + id).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/reviews/" + id + "/rerun").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/reviews/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/reviews/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidReviewRequestsAreRejected() throws Exception {
        String token = register("carol");
        mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("language", "cobol", "code", "x"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("language", "java", "code", "x".repeat(501)))))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(get("/api/reviews/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
