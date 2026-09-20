# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Fixed
- Projects could be saved with no title and no image at all (neither a URL nor
  an uploaded file), producing blank-looking cards on the public site.
  `ProjectItem.title` is now `@NotBlank` (bean validation only, no DB migration
  needed - the underlying column was never `NOT NULL`, so this is enforced at
  the API layer, same mechanism as every other content type's title/name
  field). The image field is validated as "a URL or a picked file, together" -
  something the two separate inputs couldn't express with a plain `required`
  attribute on just one of them - and blocks the save with a field-level error
  if neither is present. Also fixed field-level error messages piling up
  (duplicate text under a field) across repeated failed save attempts in the
  same modal session.
- Saving or deleting anything in the admin content page silently appeared to do
  nothing - most visibly after a project image upload, but it affected every
  section. Root cause: the success handler called `closeModal()` (which resets
  `activeSection` to `null`) *before* `loadSection(activeSection)`, so the
  reload threw immediately on a null reference - and since the modal had
  already closed by then, that error had nowhere visible to appear. Fixed by
  capturing the section/item state before closing the modal. Also added a
  single consistent toast notification ("Service saved.", "Project deleted.",
  "Order updated.", etc.) across every create/update/delete/reorder action in
  the content page, instead of some paths showing nothing and others using a
  jarring native `alert()`.
- The admin dashboard's three stat numbers (total/today/this week) were all
  plain black, hard to tell apart at a glance. Each now has its own accent
  color (a top border + matching number color): dark neutral for total, amber
  for today, teal for this week.
- `GET /api/leads` (search/filter) failed every request with a 400 - a Spring
  Data JPA 4.0 breaking change: `Specification.where(null)` *and*
  `Specification.and(null)` both now reject `null` outright (`Assert.notNull`)
  instead of treating it as "no filter" the way earlier versions did. The
  first fix (switching `LeadServiceImpl.search()`'s starting point from
  `Specification.where(...)` to `Specification.unrestricted()`) only got
  halfway there, since `LeadSpecifications`' three filter methods still
  returned `null` for an unsupplied filter, which then blew up on the very
  next `.and(...)` call ("Other specification must not be null"). Fixed
  properly by having those methods return `Specification.unrestricted()`
  instead of `null` when their filter isn't supplied, so nothing null ever
  reaches `.and()` in the first place.
- Startup failed with `Ambiguous @ExceptionHandler method mapped for
  MaxUploadSizeExceededException` - `GlobalExceptionHandler`'s own
  `@ExceptionHandler(MaxUploadSizeExceededException.class)` method collided with
  `ResponseEntityExceptionHandler`'s built-in handling for that exception type
  (it's one of the types the framework already handles by default). Fixed by
  overriding the framework's own protected hook,
  `handleMaxUploadSizeExceededException(...)`, instead of declaring a second
  `@ExceptionHandler` for the same exception - same 413 response as before.
- Admin login was unreachable on Railway ("Failed to fetch" / a browser mixed-content
  block on `http://.../admin/login?error`): Railway terminates TLS at its edge and
  forwards to the app over plain HTTP internally, so Spring/Tomcat had no way to
  know the original request was HTTPS, and built the login redirect (and the CSRF
  cookie's Secure flag) using `http://` instead. Fixed with
  `server.forward-headers-strategy: framework` (`application.yaml`), which makes
  Spring honor Railway's `X-Forwarded-Proto`/`Host`/`Port` headers. No effect
  locally, since there's no proxy there to send those headers.

### Added
- Admin dashboard at `/admin` (the new `defaultSuccessUrl` after login) - total/
  today/this-week lead counts (`GET /api/leads/stats`, backed by a new
  `LeadStats` DTO) plus quick links to the leads and content pages. All three
  admin pages now cross-link to each other.
- `GET /api/leads` gained `name`/`from`/`to` query params (case-insensitive
  substring match on name; inclusive date-range filter on submission date, all
  optional and combinable) via a new `LeadRepository`/`LeadSpecifications`
  (`JpaSpecificationExecutor`). The admin leads page has a filter toolbar (name
  search debounced, date inputs immediate, a "Clear filters" button) that resets
  to page 0 on any change.
- Drag-to-reorder for `displayOrder` on services, stats, projects, and
  testimonials in the admin content page - drag a row by its handle instead of
  typing a number. Dropped items are renumbered sequentially and only the rows
  whose order actually changed are persisted, reusing the existing generic
  `PUT {path}/{id}` endpoint (no new backend endpoint). New items in these four
  sections are appended to the end automatically.
- Image upload for Projects: the admin content page's Image field is now a
  hybrid URL-or-upload control - paste a URL as before, or pick a photo to
  upload via the new `POST /api/projects/{id}/image` (multipart, 5MB limit,
  admin-only). Uploaded bytes are stored directly in Postgres (`ProjectItem.imageData`,
  a `bytea` column, `@JsonIgnore`d so it never bloats a list response) and served
  back raw via `GET /api/projects/{id}/image` (public, day-long cache header).
  `ProjectItem.imageUrl` is no longer required at the entity level, since a
  brand-new project has neither an URL nor an uploaded image for the moment
  between creating it and uploading to it.
  **Requires a one-time manual database migration before deploying this change
  - see the "Database migration" section below.**
- Rate limit on `POST /api/leads` (the public contact form, the one write
  endpoint that doesn't require admin login): at most 5 submissions per 10
  minutes per IP address by default, configurable via
  `LEADS_RATE_LIMIT_MAX_REQUESTS` / `LEADS_RATE_LIMIT_WINDOW_MINUTES`. Going over
  it returns `429 Too Many Requests` with a `Retry-After` header instead of
  reaching the database (`filter/LeadsRateLimitFilter.java`, in-memory per-IP
  sliding window - fine for the single-instance deployment this runs as today).
  The public contact form now also shows the API's actual error message (e.g.
  this new rate-limit message, or a validation problem) instead of always a
  generic "something went wrong".
- Admin login moved from the browser's native HTTP Basic prompt to a real login
  page (`/admin/login`, `static/admin/login.html`) backed by a session cookie, with
  a working "Log out" button on both admin pages. `SecurityConfig` now uses
  `formLogin`/`logout` instead of `httpBasic`.
- CSRF protection is enabled for all admin writes (it was off, matching Basic
  auth's stateless model) using Spring Security's cookie-based SPA pattern: a
  readable `XSRF-TOKEN` cookie (`config/CsrfCookieFilter.java`) that the admin
  pages send back as an `X-XSRF-TOKEN` header (`config/SpaCsrfTokenRequestHandler.java`).
  `POST /api/leads` (the public contact form) is explicitly exempted, since
  visitors have no admin session to carry a token in.
- `GET /api/leads` is now paginated (`?page=`, `?size=`, default size 20, capped
  at 100 via `spring.data.web.pageable.max-page-size`) instead of always returning
  every lead. The admin leads page has Prev/Next controls and a page indicator.
- The admin content page's add/edit modal now surfaces field-specific validation
  errors (e.g. "title is required" under the Title field) from the API's
  `fieldErrors` response, instead of only a generic "Save failed" message.
- Admin-only content management page at `/admin/content` — add, edit, and delete
  services, stats, projects, testimonials, and company info from one page, styled to
  match the rest of the admin area. Each content type is a table with an "+ Add"
  button; editing opens a small modal form (`static/admin/content.html`). Cross-linked
  with the leads page (`/admin/leads` ↔ `/admin/content`) so admins can move between
  the two. Served at the clean URL via `AdminViewController`, same forward pattern as
  `/admin/leads`.
- `POST`/`PUT`/`DELETE` on `/api/services`, `/api/stats`, `/api/projects`,
  `/api/testimonials`, and `/api/company-info` now require the same admin login as the
  leads endpoints (`config/SecurityConfig.java`). `GET` on all of them stays public —
  the live site's own `/api/content` call depends on it.
- Admin-only leads page at `/admin/leads.html` — a styled table (matching the public
  site's theme) listing contact-form submissions newest first, with per-row delete.
  Protected by Spring Security HTTP Basic auth (`org.springframework.boot:spring-boot-
  starter-security`), configured in `config/SecurityConfig.java` with a single admin
  account from `admin.username`/`admin.password` (`ADMIN_USERNAME`/`ADMIN_PASSWORD` env
  vars — local falls back to `admin`/`changeme`, prod requires both to be set explicitly).
  `GET`/`DELETE` on `/api/leads` now require the same login; `POST /api/leads` (the
  contact form itself) and every other endpoint stay public, unchanged from before.
- Admin leads page is now reachable at the clean URL `/admin/leads` (forwards
  internally to the static `/admin/leads.html`, via `AdminViewController`), instead of
  requiring the `.html` suffix.

### Fixed
- Admin leads page showed the wrong submission time: the backend sends a naive
  `LocalDateTime` (no timezone offset), captured by the server clock (UTC on
  Railway), but browsers parse an offset-less timestamp as local time — so a lead
  submitted at 04:45 UTC displayed as "04:45 AM" instead of the correct local
  equivalent. Fixed by having the page treat the timestamp as UTC explicitly, then
  format it in a configurable display timezone.

### Added
- `app.display-timezone` config (`application.yaml`, `DISPLAY_TIMEZONE` env var,
  defaults to `Asia/Kolkata`) and a small public `GET /api/config` endpoint
  (`AppConfigController`) exposing it — the admin leads page fetches this at load
  instead of hardcoding a timezone in the page's JavaScript.


### Fixed
- Railway runtime crash: `data.sql` ran before Hibernate created the schema, failing with
  `relation "services" does not exist` during the one-time production bootstrap (schema creation +
  seed data). `application-local.yaml` already sets `defer-datasource-initialization: true`, which
  orders schema creation before data scripts, but that setting wasn't present in the temporary prod
  bootstrap environment variables. Fixed by adding `SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION=true`
  alongside `SPRING_JPA_HIBERNATE_DDL_AUTO=update` and `SPRING_SQL_INIT_MODE=always` for the bootstrap
  deploy; all three are now reverted to `validate`/`never`/`true` respectively for normal operation, so
  future deploys never re-run schema changes or reseed data.
- Railway runtime crash: datasource initialization failed with
  `Driver org.postgresql.Driver claims to not accept jdbcUrl, jdbc:postgresql://:/` — `DB_URL`
  referenced `${{Postgres.PGHOST}}`/`${{Postgres.PGPORT}}`/`${{Postgres.PGDATABASE}}`, but no Postgres
  service existed yet in the Railway project, so the reference variables silently resolved to empty
  strings instead of erroring. Fixed by provisioning a PostgreSQL service in the Railway project
  (named `Postgres`, matching the existing `${{Postgres.*}}` references) and redeploying.


### Fixed
- Railway deploy crash loop: the app built successfully but the container immediately exited with
  `ls: cannot access '*/build/libs/*jar': No such file or directory`, then printed the `java` usage/help
  text and exited — repeatedly, causing Railway's edge to return `502 Application failed to respond`
  even though "Deployment successful" showed in the dashboard (that only reflects the build/deploy
  pipeline steps, not whether the app is actually staying up). Root cause: Railpack's auto-detected
  start command for Gradle projects assumes the jar lives under a subdirectory
  (`*/build/libs/*.jar`, e.g. a Gradle multi-module layout), but this is a single-module project with
  the jar directly at `build/libs/*.jar` — the glob never matched anything. Fixed by adding a
  `railway.json` with an explicit `deploy.startCommand` that finds the built (non-`-plain`) jar under
  `build/libs/` directly, plus a restart-on-failure policy.


### Fixed
- Railway build failure: `:bootJar FAILED` — "Main class name has not been configured and it could
  not be resolved from classpath". Root cause: `BuildproApplication.main` was declared
  `static void main(String[] args)` (missing `public`) after the earlier rename from
  `SampleStarterApplication`. Spring Boot's Gradle plugin locates the main class by scanning compiled
  classes for a `public static void main(String[])` method — a package-private one is invisible to
  that scan, so `bootJar` couldn't resolve `mainClass` even though `compileJava`/`resolveMainClassName`
  had already succeeded. This wasn't just a Railway quirk: the JVM itself requires `public static void
  main` to launch a class, so the built jar would have failed the same way with `java -jar` locally.
  Fixed by adding the missing `public` modifier in `BuildproApplication.java`.


### Changed
- Replaced the Gradle Java toolchain (which required auto-downloading a specific JDK on any machine
  that didn't already have it) with plain `sourceCompatibility`/`targetCompatibility = 21` in
  `build.gradle`. The `org.gradle.toolchains.foojay-resolver-convention` plugin added to fix the
  previous Railway build failure did not actually resolve it — the identical "Toolchain download
  repositories have not been configured" error recurred on the next build, pointing at Railway's build
  sandbox not reaching the JDK download API at all. Removing the toolchain requirement sidesteps the
  problem entirely: Gradle now just compiles with whatever JDK is already running it (your local JDK 25,
  Railway's pre-installed JDK 21), with `sourceCompatibility`/`targetCompatibility` keeping the compiled
  bytecode at a consistent, widely-supported level regardless of which JDK did the compiling. Removed
  the now-unused foojay-resolver-convention plugin from `settings.gradle`.



### Fixed
- Railway build failure: "Cannot find a Java installation ... matching languageVersion=25.
  Toolchain download repositories have not been configured." Railway's build container only ships
  JDK 21, and Gradle had no way to fetch JDK 25 for the toolchain. Added the
  `org.gradle.toolchains.foojay-resolver-convention` plugin (`settings.gradle`), which lets Gradle
  auto-download the exact JDK a toolchain asks for on any machine that doesn't already have it —
  Railway, other CI, or a new teammate's laptop — without changing the JDK 25 requirement itself.



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
