# AI Code Review Assist

**🔗 Live demo: [ai-code-review-assist.onrender.com](https://ai-code-review-assist.onrender.com)**. It's on Render's free plan, so the first load after it has been idle can take up to a minute.

A full-stack app that reviews source code with AI and returns a **structured** report instead of a wall of text. Every issue has a severity, a category, the exact line it starts on and a concrete fix. You also get a corrected version of the whole file, shown as a diff against your original.

[![CI](https://github.com/Abhishek360007/AI-Code-Review-Assist/actions/workflows/ci.yml/badge.svg)](https://github.com/Abhishek360007/AI-Code-Review-Assist/actions/workflows/ci.yml) ![stack](https://img.shields.io/badge/Java-17-orange) ![stack](https://img.shields.io/badge/Spring%20Boot-3.3-6db33f) ![stack](https://img.shields.io/badge/React-18-61dafb) ![stack](https://img.shields.io/badge/PostgreSQL-16-336791) ![stack](https://img.shields.io/badge/Docker-compose-2496ed)

![Review page: score, severity-ranked findings and highlighted lines](docs/screenshots/review.png)

## Features

- **Structured AI reviews.** Gemini is called with a JSON response schema, so the output always parses: a score from 0 to 100, a summary, strengths, and a list of issues (`CRITICAL` → `INFO`, plus a category such as bug, security or performance).
- **Line-accurate findings.** The code is sent with line numbers. Click a finding and the editor jumps to that line; every flagged line is highlighted in its severity colour.
- **Suggested version.** A Monaco diff view shows your original code next to the AI's corrected version, with one-click copy.
- **History.** Search, filter by language, paginate, re-run a review or delete it.
- **Insights dashboard.** Score trend, findings by severity and category, and average score per language.
- **Offline demo mode.** With no API key set, a built-in rule engine produces reviews so the app works out of the box (useful for demos and CI).
- **16 languages.** Java, Python, JavaScript, TypeScript, C, C++, C#, Go, Rust, Kotlin, PHP, Ruby, Swift, SQL, Shell and plain text.
- **Light and dark themes.** The editor draft autosaves in the browser.
- **Resilient AI calls.** Overloaded or slow models are retried, then backup models are tried in order. Each review records which model produced it.

## Screenshots

| Suggested version (diff) | Insights |
|---|---|
| ![Diff of the original code against the AI's corrected version](docs/screenshots/diff.png) | ![Insights dashboard with score trend and breakdowns](docs/screenshots/insights.png) |
| **New review** | **History** |
| ![Code editor with language picker and review options](docs/screenshots/new-review.png) | ![Review history table with scores and worst severity](docs/screenshots/history.png) |

## Giving a demo

Every language has a built-in demo snippet with realistic, deliberate flaws. Pick a language and the editor loads its sample automatically (or click **Load sample**):

| Language | What the reviewer should catch |
|---|---|
| JavaScript | SQL injection, hard-coded API key, off-by-one crash, loose equality |
| TypeScript | Missing `await`, `any` cache, async `forEach` that doesn't wait, unsafe `!` |
| Python | SQL injection, `pickle` deserialization, bare `except`, mutable default arguments |
| Java | SQL injection, leaked JDBC resources, `!=` on strings, string concatenation in a loop |
| C++ | Buffer overflow, `delete` vs `delete[]`, returning a pointer to a local, off-by-one |
| C | `gets`, off-by-one `malloc`, format-string bug, memory leak |
| C# | `async void`, undisposed connections, SQL injection, swallowed exceptions |
| Go | Ignored errors, data race, `defer` in a loop, nil-response panic |
| Rust | `unwrap` panics, index out of bounds, needless clones, `unsafe` on empty strings |
| Kotlin | SQL injection, `!!`, `GlobalScope`, uninitialized `lateinit` |
| PHP | SQL injection, XSS, MD5 passwords, open redirect |
| Ruby | N+1 queries, mass assignment, SQL injection, `eval`, open redirect |
| Swift | Force unwraps, retain cycle, UI updates off the main thread, `try!` |
| SQL | `SELECT *`, non-sargable `YEAR()`, `UPDATE` without `WHERE`, `= NULL`, storing CVVs |
| Shell | Unquoted variables, `rm -rf $VAR/*`, hard-coded token, `curl -k`, `chmod 777`, `eval` |
| Other | A Dockerfile with `:latest`, a baked-in secret and running as root |

A suggested five-minute flow:

1. **New review:** pick a language and click **Run review**.
2. **Review page:** point out the score, then click a finding and the editor jumps to the highlighted line.
3. **Suggested version:** show the diff of the AI's corrected code.
4. Run two or three more languages, then open **Insights** to show the score trend and breakdowns.

Tip: on the free hosting plan, open the site a minute before the demo to wake it up.

## Architecture

```
┌──────────────┐   /api/*   ┌────────────────────┐   HTTPS   ┌──────────────┐
│  React + Vite│ ─────────▶ │ Spring Boot 3 API  │ ────────▶ │ Gemini API   │
│  (nginx)     │            │ JWT · JPA · Valid. │           └──────────────┘
└──────────────┘            └─────────┬──────────┘
                                      │
                               ┌──────▼──────┐
                               │ PostgreSQL  │
                               └─────────────┘
```

```
server/   Spring Boot API
  ai/         CodeAnalyzer interface, GeminiAnalyzer (structured output), DemoAnalyzer (rules)
  auth/       register / login / me
  review/     Review entity + findings, service, controller, stats, rate limiter
  security/   JWT issue/verify + request filter
  config/     typed app properties, security & CORS
web/      React SPA (Vite, React Router, Monaco editor)
```

## Quick start (Docker)

```bash
cp .env.example .env
# edit .env: set JWT_SECRET (openssl rand -base64 48) and, optionally, GEMINI_API_KEY
docker compose up --build
```

Open **http://localhost:3000**, create an account and run a review. The API is also available on `http://localhost:8080`.

## Local development

**Requirements:** JDK 17+, Node 20+, and PostgreSQL 14+ (or just `docker compose up db`).

```bash
# 1. Database
docker compose up -d db          # or create a local DB: reviewassist / reviewassist

# 2. API (http://localhost:8080)
cd server
export JWT_SECRET="$(openssl rand -base64 48)"
export GEMINI_API_KEY="your-key"   # optional
./mvnw spring-boot:run

# 3. Web (http://localhost:5173, proxies /api to :8080)
cd web
npm install
npm run dev
```

Run the backend tests (they use in-memory H2 and the demo analyzer, so no keys are needed):

```bash
cd server && ./mvnw test
```

## Deploy (Render + Neon, free tier)

The app runs as a single service on [Render](https://render.com), built from the root [`Dockerfile`](Dockerfile), which serves the web app and the API from one URL. Data lives in a serverless PostgreSQL database on [Neon](https://neon.tech).

1. **Database:** create a free project on [neon.tech](https://neon.tech) and copy its connection string (`postgresql://…?sslmode=require`).
2. **App:** sign in to Render with GitHub, go to **New → Blueprint** and select this repository. The [`render.yaml`](render.yaml) Blueprint is picked up automatically.
3. When prompted, paste `DATABASE_URL` (the Neon string) and `GEMINI_API_KEY`. `JWT_SECRET` is generated for you.
4. Click **Apply** and wait for the first build (about 5–10 minutes). Tables are created on first start.

Every push to `main` redeploys automatically. Free services sleep when idle and Neon scales to zero, so the first request after a quiet period takes around 30–60 seconds.

`DATABASE_URL` accepts any standard Postgres connection string, so Supabase, Railway or a self-hosted server work the same way.

## Configuration

All settings come from environment variables:

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | none (**required**) | HMAC signing key, at least 32 characters |
| `JWT_TTL` | `12h` | Token lifetime |
| `GEMINI_API_KEY` | empty | Google AI Studio key. Leave empty for demo mode |
| `GEMINI_MODEL` | `gemini-flash-latest` | Any Gemini model that supports structured output |
| `GEMINI_FALLBACK_MODELS` | `gemini-flash-lite-latest,gemini-3.5-flash` | Tried in order if the main model is overloaded (with automatic retry) |
| `AI_PROVIDER` | `auto` | `auto`, `gemini` or `demo` |
| `DATABASE_URL` | empty | Postgres connection string, e.g. from Neon: `postgresql://user:pass@host/db?sslmode=require`. Takes priority over the `DB_*` variables |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | local `reviewassist` | Alternative: JDBC URL plus credentials (or `DB_HOST` / `DB_PORT` / `DB_NAME`) |
| `DB_POOL_SIZE` | `5` | Maximum database connections |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `REVIEW_MAX_CODE_CHARS` | `20000` | Maximum size of submitted code |
| `REVIEW_PER_MINUTE_LIMIT` | `10` | Reviews per user per minute (`0` turns the limit off) |

## API

All `/api/reviews` endpoints need `Authorization: Bearer <token>`. Errors use the RFC 7807 `application/problem+json` format.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create an account and get a token |
| `POST` | `/api/auth/login` | Log in with username **or** email |
| `GET` | `/api/auth/me` | Current user |
| `GET` | `/api/meta` | Active engine, size limit, supported languages (public) |
| `POST` | `/api/reviews` | Run a review: `{ language, code, title?, notes? }` |
| `GET` | `/api/reviews?page=&size=&language=&q=` | Paginated history |
| `GET` | `/api/reviews/{id}` | Full review |
| `POST` | `/api/reviews/{id}/rerun` | Review the same code again |
| `DELETE` | `/api/reviews/{id}` | Delete a review |
| `GET` | `/api/reviews/stats` | Aggregated insights |

Example response (trimmed):

```json
{
  "id": 42,
  "score": 46,
  "summary": "The function works for the happy path but has an injection risk and an off-by-one bug.",
  "findings": [
    {
      "severity": "CRITICAL",
      "category": "SECURITY",
      "line": 4,
      "title": "SQL injection via string concatenation",
      "detail": "userId is concatenated directly into the query string.",
      "fix": "db.query('SELECT * FROM orders WHERE user_id = ?', [userId])"
    }
  ],
  "strengths": ["Uses async/await consistently"],
  "improvedCode": "async function getOrderTotal(db, userId) { ... }",
  "engine": "gemini-flash-latest",
  "durationMs": 3120
}
```

## Security notes

- Secrets live only in environment variables. Nothing sensitive is committed, and the app refuses to start without a strong `JWT_SECRET`.
- The Gemini key is sent in the `x-goog-api-key` header, never in the URL.
- Passwords are hashed with BCrypt. Login returns the same error for an unknown account and a wrong password.
- Reviews are scoped to their owner: other users get `404`, not `403`.
- Code size limits and a per-user rate limit protect the AI quota.
- The prompt tells the model to treat submitted code strictly as data, which reduces prompt-injection risk.

## Roadmap

- GitHub pull request integration (review a diff by URL)
- Streaming responses for long files
- Team workspaces and shared reviews

## Author

**Abhishek Raj** · [GitHub @Abhishek360007](https://github.com/Abhishek360007)

## License

[MIT](LICENSE)
