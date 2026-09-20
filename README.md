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
| Services | `/api/services` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\* |
| Stats | `/api/stats` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\* |
| Projects | `/api/projects` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\* |
| Testimonials | `/api/testimonials` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\* |
| Company info | `/api/company-info` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\* |
| Leads (contact form) | `/api/leads` | GET\* (paginated, `?page=&size=`), GET/{id}\*, POST†, DELETE/{id}\* (no PUT) |
| Combined content | `/api/content` | GET — everything above in one call, what the page itself fetches |

\* Requires the admin login (see Admin area below). Reading content (`GET`) and
submitting the contact form (`POST /api/leads`) stay public — the live site and its
visitors depend on both.

† `POST /api/leads` is public but rate-limited: at most 5 submissions per 10
minutes per IP address (configurable via `LEADS_RATE_LIMIT_MAX_REQUESTS` /
`LEADS_RATE_LIMIT_WINDOW_MINUTES`), to keep the open contact form from being spammed.
Going over it gets a `429` with a `Retry-After` header instead of reaching the
database — see `filter/LeadsRateLimitFilter.java`.

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

Four admin-only pages, cross-linked to each other, all guarded by a single admin
account:

- **`/admin/login`** — sign in with the admin username/password. A real login page
  (not the browser's native HTTP Basic prompt), backed by a session cookie — so
  there's an actual "Log out" button, unlike Basic auth, where the browser just
  keeps resending cached credentials forever with no way to sign out.
- **`/admin`** — dashboard: total/today/this-week lead counts (`GET /api/leads/stats`)
  and quick links to the leads and content pages. This is where a successful login
  now lands (`defaultSuccessUrl`), unless you were redirected here mid-visit to
  another admin page, in which case you land back there instead.
- **`/admin/leads`** — view and delete contact form submissions, paginated (20 per
  page, newest first, with Prev/Next controls) and filterable by name (substring,
  debounced) and/or a from/to submission-date range (`GET /api/leads?name=&from=&to=`).
- **`/admin/content`** — add, edit, and delete everything shown on the public site:
  services, stats, projects, testimonials, and company info. Each section is a table
  with an "+ Add" button; editing opens a small form in a modal, which shows
  field-specific validation errors (e.g. "title is required" under the Title field)
  instead of only a generic failure message. Changes go live immediately, since the
  public page reads the same data via `/api/content`.
  - Services, stats, projects, and testimonials support drag-to-reorder: drag a row
    by its handle to change `displayOrder` instead of typing a number. Only the
    rows whose order actually changed are saved (existing `PUT {path}/{id}`, no new
    endpoint); new items are appended to the end automatically.
  - Projects have an image upload option: the Image field takes either a pasted URL
    or a picked file. An uploaded file is sent to `POST /api/projects/{id}/image`
    (multipart, 5MB max) and stored as bytes directly in Postgres, served back via
    `GET /api/projects/{id}/image` (public, cached a day). See "Database migration"
    below — this needs a one-time schema change before it'll work.

All three are clean-URL forwards to their static pages (`/admin/login.html`,
`/admin/leads.html`, `/admin/content.html`, see `AdminViewController`) so the address
bar never shows the `.html` suffix. Everything else — the public site, `/api/content`,
and submitting the contact form itself — stays open, same as before.

Credentials come from `admin.username` / `admin.password` (`application.yaml`),
backed by `ADMIN_USERNAME` / `ADMIN_PASSWORD` env vars:

- **Local:** falls back to `admin` / `changeme` if the env vars aren't set — fine for
  local dev, change it if you'll leave the app running somewhere reachable.
- **Prod:** both env vars are required — startup fails loudly if either is missing,
  rather than silently running with the local default.

Signing in via `/admin/login` creates a session (a `JSESSIONID` cookie); "Log out" on
either admin page ends it and returns to the login page. Because auth is now
session/cookie-based rather than stateless Basic auth, CSRF protection is on for
every admin write — the admin pages read a CSRF token from a readable cookie
(`XSRF-TOKEN`) and send it back as an `X-XSRF-TOKEN` header on every `POST`/`PUT`/
`DELETE` (see `config/CsrfCookieFilter.java` and `config/SpaCsrfTokenRequestHandler.java`).
The public contact form (`POST /api/leads`) is explicitly exempted from CSRF, since
visitors submitting it have no admin session to carry a token in.

The `GET`/`DELETE` endpoints on `/api/leads` require the same admin login (submitting
the form via `POST /api/leads` stays public, since visitors use it with no account).
Likewise, `POST`/`PUT`/`DELETE` on `/api/services`, `/api/stats`, `/api/projects`,
`/api/testimonials`, and `/api/company-info` require the admin login — `GET` on all
of them stays public, since the live site's own `/api/content` call depends on it.
A fetch call from the admin pages that isn't signed in (e.g. an expired session)
gets a clean `401` rather than a redirect, so the page can show "not signed in"
instead of a broken response; a direct browser visit to a protected admin page still
redirects to `/admin/login` as expected.

The leads page displays submission times in a configurable timezone
(`app.display-timezone` in `application.yaml`, backed by the `DISPLAY_TIMEZONE` env
var, defaulting to `Asia/Kolkata`) — fetched at page load from `GET /api/config`, not
hardcoded in the page itself. Submission times are stored on the server in UTC (the
JVM's clock on Railway), so this only affects how they're *displayed*.

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

### Database migrations (Liquibase)

`spring.jpa.hibernate.ddl-auto` is `validate` in both profiles (see
`application-local.yaml` / `application-prod.yaml`) — Hibernate never changes the
schema itself, it only checks the JPA mappings still match whatever's actually
there. **Liquibase is what owns the schema now**, including seed data - there's
no `data.sql`/`spring.sql.init` anymore. Changesets live under
`src/main/resources/db/changelog/`, included from `db.changelog-master.yaml`:

- `001-baseline-schema.yaml` - `createTable` for all 6 tables.
- `002-seed-data.yaml` - the original static site's demo content (services,
  stats, demo projects, testimonials, a placeholder company info row).

Spring Boot runs pending changesets automatically on every startup (local and
Railway) and tracks which ones have already run per-database in its
`DATABASECHANGELOG` table, so each changeset - schema **or** seed data -
executes exactly once, ever, per database, never again after that. That's a
meaningful difference from the old `data.sql` approach: `data.sql` used
`ON CONFLICT (id) DO NOTHING`, which protected an edited row from being
overwritten but did nothing to stop a *deleted* row from being silently
recreated on the next restart (delete it -> no more conflict -> `data.sql`
just re-inserts it next time it runs). A Liquibase changeset has no such gap -
delete a seed row through the admin panel and it's gone for good, the same as
deleting anything else.

Going forward, a schema change **or** a data change is a new changeset file
under `db/changelog/changes/`, included from the master changelog - not
hand-running SQL against production like the old `image_data`/
`image_content_type` migration below, and not another `data.sql` edit.

**One-time setup:** Liquibase was introduced onto databases (both local and
Railway) that already had a live, unmanaged schema. Rather than baselining that
existing schema, both databases were wiped and rebuilt from scratch, so
`001-baseline-schema.yaml`'s `createTable` changesets (and `002-seed-data.yaml`'s
inserts) run for real, instead of just being marked as already-applied, and
Liquibase's own tracking table starts clean. **This deletes all existing data**
(leads, services, stats, projects, testimonials, company info) - it was done
deliberately here since there was nothing in either database worth keeping at
the time, not something to repeat casually against a database with real data in
it later. It's also required once more after `002-seed-data.yaml` was
introduced: a database that already has the seed rows from the old `data.sql`
(inserted directly, outside Liquibase's tracking) will hit a duplicate-key error
the moment Liquibase tries to insert those same ids for real.

```sql
-- Run against the target Postgres database (local: psql -U postgres -h
-- localhost -d buildpro; Railway: open the Postgres service -> Data/Query tab,
-- or psql using the connection string from the Connect tab). Dropping and
-- recreating the "public" schema wipes every table without needing
-- database-level create/drop privileges (Railway's managed Postgres role
-- usually only has schema-level rights on its own database):
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO public;
```

Then start the app - Liquibase runs automatically on startup, before Hibernate's
`ddl-auto: validate` check, creates all 6 tables from `001-baseline-schema.yaml`,
and seeds them from `002-seed-data.yaml`, in that order, every time (local and
Railway) - no separate manual reseed step.

**Liquibase / Spring Boot version note:** the app crashed on every single
startup attempt (both locally and on Railway) with `BeanCreationException:
Circular depends-on relationship between 'liquibase' and
'entityManagerFactory'` under Spring Boot **4.0.4**. Two attempts to fix this
by pinning `org.liquibase:liquibase-core` in `build.gradle` (a plain
`implementation` declaration, then a `resolutionStrategy.force`) either had no
effect or - once the version was genuinely forced to 4.33.0 via
`resolutionStrategy.force` - crashed with the byte-for-byte identical error,
proving the Liquibase-core version was never the actual cause. The real cause
is a Spring Boot 4.0.x autoconfiguration bug: `LiquibaseAutoConfiguration`
imports `DatabaseInitializationDependencyConfigurer`, which wires the
`dependsOn` relationships between database-initializer beans (Liquibase) and
their dependents (`entityManagerFactory`) - in 4.0.4 that wiring ends up
pointing both directions for this app's bean combination. The fix was to bump
`org.springframework.boot` from **4.0.4 to 4.0.8** (the latest 4.0.x patch as
of writing) in `build.gradle`, with no manual `liquibase-core` version pin -
Boot's own dependency-management BOM picks the matching `liquibase-core`
version automatically. If a future Boot upgrade reintroduces this error, treat
it as this same autoconfiguration bug resurfacing rather than reaching for a
`liquibase-core` version pin again - check for a newer 4.0.x/4.1.x patch
first.

## Changelog

See `CHANGELOG.md` for the full history of changes to this project.
