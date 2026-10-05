
# TRUE Community

A community-driven **fact-checking platform** built as a Spring Boot MVC course project
(Internet Programming B). Users vote on the truthfulness of public statements with optional
evidence, the system computes a live consensus per verdict and per topic, and an editor
publishes a final verdict. A **trust-score** mechanism rewards or penalizes voters depending
on whether their vote matched the editor's final verdict.

The UI is server-rendered with **Thymeleaf** (RTL Hebrew). A small REST API under `/api/**`
exposes statement reads, consensus, voting, comments, editor decisions and the leaderboard.

## Authors

Developed by **Noor Salah** and **Amer Abu Sair** as university coursework. This is an independent portfolio copy; the original Classroom submission is preserved.

## Tech stack

- **Java 25**, **Spring Boot 4.0.6** (Web MVC, Thymeleaf, Spring Security, Spring Data JPA, Validation)
- **MySQL 8** (schema `ex4`), Hibernate `ddl-auto=update`
- Maven wrapper (`mvnw` / `mvnw.cmd`)
- Bootstrap 5 (RTL) via CDN

## Requirements

- **Java 25** (`java -version` should report 25.x)
- **Docker** (for MySQL + phpMyAdmin), or a local MySQL 8 with a schema named `ex4`

## Database configuration

Defined in [src/main/resources/application.properties](src/main/resources/application.properties):

| Setting   | Value                                   |
|-----------|-----------------------------------------|
| Schema    | `ex4`                                   |
| URL       | `jdbc:mysql://localhost:3306/ex4`       |
| Username  | `root`                                  |
| Password | Set the `DB_PASSWORD` environment variable |
| ddl-auto  | `update` (Hibernate creates/migrates tables) |

> These are **local demo credentials** for a course project, not production secrets.

## Run with Docker (recommended)

```bash
# 1. Start MySQL (port 3306) + phpMyAdmin (port 8081)
docker compose up -d

# 2. Run the application (Windows: mvnw.cmd)
./mvnw spring-boot:run

# 3. Stop the database containers when done
docker compose down            # keep data
docker compose down -v         # also delete the MySQL volume (clean slate)
```

- **Application:** http://localhost:8080
- **phpMyAdmin:** http://localhost:8081 (server `mysql`, user `root`, your `DB_PASSWORD` value)

On first start, [DataInitializer](src/main/java/com/truecommunity/config/DataInitializer.java)
seeds (idempotently) 10 topics, a default editor, 30 demo users, and 12 sample statements
with votes and comments (8 pending, 2 under review, 2 decided).

## Seeded accounts

| Role   | Email / login (identity is the **email**) | Password      |
|--------|-------------------------------------------|---------------|
| Editor | `editor@truecommunity.local`              | `editor123`   |
| User   | `user1@example.com` … `user30@example.com`| `password123` |

New users register at `/register` and are always created with the `USER` role
(self-registration as editor is not possible).

## Build & test

```bash
./mvnw clean package          # build the jar
./mvnw test                   # run tests (none exist yet - see technical debt)
java -jar target/true-community-0.0.1-SNAPSHOT.jar
```

Example first run (empty DB): start Docker, run `./mvnw spring-boot:run`, open
<http://localhost:8080>, log in as `editor@truecommunity.local` / `editor123`, claim a
pending statement on the editor desk, then publish a verdict.

### JavaDoc

JavaDoc is written inline (English) on classes, interfaces, and methods throughout the source.
Generate the browsable HTML with:

```bash
./mvnw javadoc:javadoc        # output under target/reports/apidocs/index.html
```

The generated HTML is **not** committed to the repository (regenerate it on demand).

## Main features

- **Discovery feed** — statements split into "available to vote", "awaiting verdict"
  (you voted), and "resolved", with search/filter by keyword, status and topic.
- **Voting arena** — one vote per user per statement, with a verdict, a single crowdsourced
  topic suggestion, optional explanation and (http/https) evidence link. Community stats are
  hidden from a user until they vote (anti-bias). Live verdict consensus and per-topic consensus.
- **Comments** — public discussion per statement with up/down votes.
- **Editor desk** — editors claim statements, review the top/bottom community comments and
  highest-trust voters, then publish a final verdict (which updates trust scores). Decisions
  can be reverted.
- **Trust score & leaderboard** — score starts at `1.0`, clamped to `[0.0, 5.0]`; +0.10 for a
  vote matching the final verdict, -0.05 for a mismatch. Public leaderboard + personal rank.
- **Personal dashboard** — the signed-in user's voting history and trust trajectory.
- **Sessions** — several `@SessionScope` beans persist per-user UI state across requests:
  recent searches, recently-viewed statements, the dashboard history filter, admin
  search filters, and unsaved vote / decision / add-statement drafts.

## Routes overview

| Type            | Examples |
|-----------------|----------|
| Public pages    | `/`, `/feed`, `/arena/{id}`, `/leaderboard`, `/login`, `/register` |
| Authenticated   | `/dashboard`, POST `/arena/{id}/vote`, POST `/arena/{id}/comment`, POST `/arena/comments/{id}/vote` |
| Editor only     | `/editor/**` |
| Public REST     | `GET /api/statements/**`, `GET /api/users/leaderboard` |
| Authenticated REST | `GET /api/users/{id}`, `POST /api/statements/{id}/votes`, `POST /api/statements/{id}/comments` |
| Editor REST     | `POST` / `GET /api/editor/statements/{id}/decision` |

## API tests

Two REST-client scripts (IntelliJ HTTP Client / VS Code "REST Client"):

- [http/api-tests.http](http/api-tests.http) — endpoint-by-endpoint coverage of the real API,
  including validation, duplicates, 404/409/400/403/405/415 cases and authorization edges.
  **This is the up-to-date script** (matches the current Hebrew seed data).
- [http/true-community-api.http](http/true-community-api.http) — an older ordered run,
  kept for reference; its search examples use stale English values.

To run: open either file in the IntelliJ HTTP Client or the VS Code "REST Client" extension,
start MySQL + the app, then execute the requests top-to-bottom (the client keeps the login
cookie for the authenticated calls).

> **CSRF note:** Spring Security CSRF protection is **enabled** (this is a server-rendered MVC
> app; the Thymeleaf forms carry the token). Token-less JSON `POST`s return `403`. To exercise
> the JSON write endpoints from a plain REST client, run them through a browser session or
> temporarily relax CSRF for `/api/**` in `SecurityConfig` for **local testing only** — do not
> commit that change.

## Known technical debt

- **No automated tests yet.** `spring-boot-starter-test` is on the classpath but no test
  classes exist; service-layer unit tests would be the highest-value addition.
- **`reports` feature is not implemented.** The `Report` entity/repository scaffolding was
  removed during cleanup because nothing wired it (no service/controller/UI) and it only
  produced an unused table. Re-introduce it with a full slice if reporting is needed.
- **No standalone REST endpoints** for topics, user activity logs, or trust-score logs.
  Activity and trust logs are written and surfaced in the editor desk / dashboard views but
  are not exposed as JSON APIs.
- **SSE live updates are implemented** (feed, arena, dashboard, editor desk, leaderboard,
  admin pages). They degrade gracefully to a normal page load when `EventSource` is unavailable.

## Project layout

Layered Spring MVC under `com.truecommunity`:
`controllers` → `services` (interface + `*Impl`) → `repo` (Spring Data JPA) → `model` (JPA
entities). DTOs, exceptions, security, session and config live in their own packages.
See [CLAUDE.md](CLAUDE.md) for conventions (static factory methods, email-as-identity,
`AuthenticatedUserProvider`, dual REST/MVC exception advice).

## Public portfolio setup

Set `DB_PASSWORD` in the shell used for both Docker Compose and Maven before starting. A database dump is intentionally excluded; Hibernate and DataInitializer create the local demo data. Seeded accounts listed above are local demonstration accounts only. This is an academic project, separate from professional or volunteer work on similarly named services.
