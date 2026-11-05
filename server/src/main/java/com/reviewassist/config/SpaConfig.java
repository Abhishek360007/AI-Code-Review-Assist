package com.reviewassist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * In the single-image deployment the built React app is bundled into {@code classpath:/static}.
 * Real files are served as-is; any other non-API path falls back to index.html so client-side
 * routes like /reviews/42 work on a hard refresh. Without a bundled UI this is a no-op.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (path.startsWith("api/") || path.startsWith("actuator/") || !INDEX.exists()) {
                            return null; // real 404
                        }
                        return INDEX;
                    }
                });
    }
}
