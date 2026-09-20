# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Changed
- Renamed the project from `sample_starter` to `buildpro`, end to end:
  - `settings.gradle` (`rootProject.name`), `build.gradle` (`description`)
  - Java package `com.example.sample_starter` → `com.example.buildpro` (every class moved and
    re-packaged)
  - Main class `SampleStarterApplication` → `BuildproApplication` (and its test class)
  - `spring.application.name` in `application.yaml`, and the `com.example.buildpro` logger package in
    all three profile files
  - OpenAPI title (Swagger UI) → "BuildPro API"
  - IntelliJ run config → `.idea/runConfigurations/BuildproApplication.xml`
  - The local Postgres database itself was intentionally left named `sample_starter` — renaming a live
    database is a separate, riskier step and wasn't part of this change.


### Added
- Swagger / OpenAPI docs via `springdoc-openapi-starter-webmvc-ui:3.1.1`. Once the app is running:
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`
  - `OpenApiConfig` sets the API title/description shown in the UI.
- `CHANGELOG.md` (this file).
- `RequestLoggingFilter` (`filter/RequestLoggingFilter.java`) — tags every request with a correlation
  id (`X-Request-Id` header, generated if the caller doesn't send one), puts it in MDC so it shows up in
  every log line for that request via the new `logging.pattern.console` pattern, and logs
  method/path/status/duration. Filters out noisy paths (static assets, Swagger UI, `/v3/api-docs`) so
  page loads don't flood the log with asset requests.
- Global exception handling (`exception/GlobalExceptionHandler.java`, `@RestControllerAdvice`) — turns
  validation failures, malformed JSON, data-integrity violations, and any unhandled exception into a
  consistent `ApiError` JSON body (`timestamp`, `status`, `error`, `message`, `path`, and `fieldErrors`
  for validation failures) instead of Spring's default whitelabel error page. Unhandled exceptions are
  logged with the full stack trace before returning a generic 500 message to the caller.
- `exception/ResourceNotFoundException.java` — available for any service/controller that would rather
  throw than return `Optional.empty()`; the handler turns it into a 404.
- Bean validation on the entities backing request bodies (`@NotBlank`/`@NotNull`/`@Email`), and `@Valid`
  on every `POST`/`PUT` controller method, so the new validation-error handling actually gets exercised.

### Added (JSON-only enforcement)
- Every controller's `@RequestMapping` now declares `produces = MediaType.APPLICATION_JSON_VALUE` and
  `consumes = MediaType.APPLICATION_JSON_VALUE` — the API only accepts JSON request bodies and only
  returns JSON responses.
- `GlobalExceptionHandler` now overrides `handleHttpMediaTypeNotSupported` (415, for a non-JSON
  `Content-Type` on a `POST`/`PUT` body) and `handleHttpMediaTypeNotAcceptable` (406, for an `Accept`
  header that excludes JSON), both returning the same `ApiError` shape as every other error.

### Fixed
- Swagger UI (`/swagger-ui.html`) and the raw OpenAPI doc (`/v3/api-docs`) are now disabled in
  `application-prod.yaml` (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`) —
  they were on by default in every profile, including prod, which springdoc warns about on startup.
  Still fully available under the `local` profile.

### Fixed
- The JSON-only enforcement above had a bug: `consumes = MediaType.APPLICATION_JSON_VALUE` was set on
  the class-level `@RequestMapping`, so it applied to `GET`/`DELETE` requests too — which have no body
  at all — and Spring rejected them with `415 Unsupported Media Type`. This broke every `GET` endpoint,
  including `/api/content`, which is why the page loaded but every dynamic section stayed empty.
  `consumes` now lives only on the `@PostMapping`/`@PutMapping` methods that actually take a request
  body; `produces` stays at the class level since it's valid for every method.

### Changed
- Refactored every controller from talking to repositories directly to a service layer:
  `service/<Entity>Service` (interface) + `service/impl/<Entity>ServiceImpl` (implementation), for
  `ServiceItem`, `Stat`, `ProjectItem`, `Testimonial`, `CompanyInfo`, `Lead`, plus a `SiteContentService`
  backing the combined `/api/content` endpoint. Controllers now depend only on the service interfaces;
  repositories are used exclusively inside the `*ServiceImpl` classes.
- Every REST endpoint now carries `@Tag`/`@Operation` annotations so they show up grouped and described
  in Swagger UI.

## [0.3.0] - API + Postgres-backed content

### Added
- JPA entities `ServiceItem`, `Stat`, `ProjectItem`, `Testimonial`, `CompanyInfo`, `Lead` mapped to
  `services`, `stats`, `projects`, `testimonials`, `company_info`, `leads` tables.
- Full CRUD REST endpoints: `/api/services`, `/api/stats`, `/api/projects`, `/api/testimonials`,
  `/api/company-info` (GET all, GET by id, POST, PUT, DELETE); `/api/leads` (GET all, GET by id, POST,
  DELETE — no PUT, a submission shouldn't be silently rewritten).
- `GET /api/content` — combined endpoint returning services, stats, projects, testimonials and company
  info in a single response, used by the frontend instead of five separate calls.
- `src/main/resources/data.sql` — idempotent seed data (explicit ids + `ON CONFLICT DO NOTHING`) matching
  the original static page content, safe to re-run on every app start.
- The uploaded BuildPro Construction page moved to `src/main/resources/static/index.html`, served at
  `/`. Its hardcoded services/stats/projects/testimonials/contact-info are now rendered client-side from
  `GET /api/content`, and the contact form posts to `POST /api/leads` instead of doing nothing.

### Changed
- `build.gradle`: dropped `spring-boot-starter-jdbc` in favor of `spring-boot-starter-data-jpa`; added
  `spring-boot-starter-validation`, the Postgres driver (`runtimeOnly`), and Lombok
  (`compileOnly`/`annotationProcessor`).
- `application-local.yaml`: added `spring.jpa.hibernate.ddl-auto=update`, `show-sql=true`,
  `defer-datasource-initialization=true`, and `spring.sql.init.mode=always` so schema creation and
  seeding happen automatically against the local Postgres database.
- `application-prod.yaml`: `ddl-auto=validate` and `sql.init.mode=never` — schema changes and seeding are
  intentionally not automatic against a production database.

## [0.2.0] - Local Postgres

### Added
- Local Postgres 16 via Homebrew (`postgresql@16`), `postgres` role with password auth, `sample_starter`
  database.
- `application.yaml` split into base + `application-local.yaml` + `application-prod.yaml`, with a
  `spring.datasource` block in the local profile pointing at the local database.
- A DBeaver connection to the local database, used for manual inspection during development.
- Full write-up in the "Postgres Local Dev Setup" doc, including the Homebrew `trust`-auth default
  (DBeaver connecting with no password) and the fix (switching `pg_hba.conf` to `scram-sha-256`).

## [0.1.0] - Project bootstrap

### Added
- IntelliJ run/debug configuration (`.idea/runConfigurations/SampleStarterApplication.xml`) for
  `SampleStarterApplication`.

### Fixed
- Gradle/IntelliJ JDK mismatch: `build.gradle`'s toolchain requested JDK 26 while the IntelliJ project
  JDK was set to `ms-25`; toolchain lowered to JDK 25 to match.
