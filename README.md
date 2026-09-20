# buildpro — BuildPro Construction

A Spring Boot backend for the BuildPro Construction page: services, stats, project
showcase, testimonials, company info and a contact form, all stored in Postgres and
served through a REST API. The static page itself is served by the app and populated
from that API at load time.

## Tech stack

- Java 21 (Gradle `sourceCompatibility`/`targetCompatibility` — no toolchain, compiles with whatever JDK is already running Gradle), Spring Boot 4.0.4
- Spring Web MVC, Spring Data JPA, Bean Validation
- PostgreSQL
- springdoc-openapi (Swagger UI), Lombok
- Gradle (wrapper included, `./gradlew`)

## Project layout

```
src/main/java/com/example/buildpro/
  entity/       JPA entities (ServiceItem, Stat, ProjectItem, Testimonial, CompanyInfo, Lead)
  repository/   Spring Data JPA repositories
  service/      Service interfaces
  service/impl/ Service implementations (only these talk to repositories)
  controller/   REST controllers (only these talk to services)
  dto/          SiteContentResponse — the combined /api/content payload
  filter/       RequestLoggingFilter — correlation id + request logging
  exception/    GlobalExceptionHandler, ApiError, ResourceNotFoundException
  config/       OpenApiConfig
src/main/resources/
  application.yaml         base config (shared)
  application-local.yaml   local Postgres connection, dev logging, schema auto-update
  application-prod.yaml    validate-only schema, Swagger disabled, no auto-seeding
  data.sql                 idempotent seed data matching the original static page
  static/index.html        the page itself, fetches its content from /api/content
```

## Prerequisites

- A JDK capable of running Gradle locally (JDK 21+; the build targets Java 21 bytecode regardless of which JDK compiles it)
- PostgreSQL running locally (see below)

## Local database setup

```bash
brew install postgresql@16
brew services start postgresql@16

/opt/homebrew/opt/postgresql@16/bin/createuser -s postgres
/opt/homebrew/opt/postgresql@16/bin/psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'Hertzberger7793';"
/opt/homebrew/opt/postgresql@16/bin/createdb -U postgres buildpro
```

`src/main/resources/application-local.yaml` already points at
`jdbc:postgresql://localhost:5432/buildpro` with those credentials. On first run,
Hibernate creates the schema (`ddl-auto: update`) and `data.sql` seeds it — both are
idempotent, safe to restart repeatedly.

By default Homebrew's Postgres uses `trust` authentication locally (no password check).
If you've tightened `pg_hba.conf` to `scram-sha-256`, the app will prompt/require the
password above as normal — no config changes needed on the app side.

## Running

```bash
./gradlew bootRun
```

Runs on the `local` profile by default (`spring.profiles.active: local` in
`application.yaml`). Then open:

- The page: <http://localhost:8080/>
- Swagger UI: <http://localhost:8080/swagger-ui.html> (local profile only — disabled in prod)
- OpenAPI spec: <http://localhost:8080/v3/api-docs> (local profile only)

To run against the `prod` profile instead:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

(`prod` expects `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars, validates the schema
instead of auto-updating it, and does not seed data.)

## API

All endpoints accept and return `application/json` only (non-JSON requests get `415`,
non-JSON `Accept` headers get `406`).

| Resource | Base path | Methods |
| --- | --- | --- |
| Services | `/api/services` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id} |
| Stats | `/api/stats` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id} |
| Projects | `/api/projects` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id} |
| Testimonials | `/api/testimonials` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id} |
| Company info | `/api/company-info` | GET, GET/{id}, POST, PUT/{id}, DELETE/{id} |
| Leads (contact form) | `/api/leads` | GET\*, GET/{id}\*, POST, DELETE/{id}\* (no PUT) |
| Combined content | `/api/content` | GET — everything above in one call, what the page itself fetches |

\* Requires the admin login (see Admin area below); `POST` stays public for the contact form itself.

Full curl examples with sample requests and responses are in the "buildpro API
curl Reference" doc. Bad requests return a consistent JSON error shape via
`GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-20T08:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields.",
  "fieldErrors": {"title": "title is required"}
}
```

## Debugging

An IntelliJ run/debug configuration is checked in at
`.idea/runConfigurations/BuildproApplication.xml` — Run > Edit Configurations >
`BuildproApplication`, then use the debug (bug) icon.

## Logging

Every request is tagged with a correlation id (`X-Request-Id` header, echoed back in
the response) and logged with method/path/status/duration, via `RequestLoggingFilter`.
Static assets, Swagger UI, and the OpenAPI doc are excluded from this logging to avoid
noise. The console log pattern includes the correlation id so all lines for one request
can be found together.

## Admin area

Contact form submissions (leads) are viewable separately from the public site, at
`/admin/leads.html`, protected by a single admin account (HTTP Basic — the browser
shows its native login prompt, no custom login form). Everything else (the public
site, `/api/content`, and submitting the contact form itself) stays open, same as
before.

Credentials come from `admin.username` / `admin.password` (`application.yaml`),
backed by `ADMIN_USERNAME` / `ADMIN_PASSWORD` env vars:

- **Local:** falls back to `admin` / `changeme` if the env vars aren't set — fine for
  local dev, change it if you'll leave the app running somewhere reachable.
- **Prod:** both env vars are required — startup fails loudly if either is missing,
  rather than silently running with the local default.

The `GET`/`DELETE` endpoints on `/api/leads` require the same admin login (submitting
the form via `POST /api/leads` stays public, since visitors use it with no account).

## Deployment

The app is deployed on [Railway](https://railway.app), running the `prod` profile
against a Railway-managed Postgres service. Live URL:

<https://buildpro-production-bd6d.up.railway.app/>

- Swagger/OpenAPI is disabled in `prod` (see `application-prod.yaml`) — it's only
  available when running locally under the `local` profile.
- `railway.json` (project root) pins an explicit `deploy.startCommand` — Railway's
  auto-detected Gradle start command doesn't resolve the jar correctly for this
  single-module layout, so we point it at `build/libs/*.jar` directly.
- Full write-up of the deployment steps, the issues hit along the way, and how each
  was fixed is in the "buildpro Deployment Guide" doc.

## Changelog

See `CHANGELOG.md` for the full history of changes to this project.
