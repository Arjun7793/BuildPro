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
  entity/       JPA entities (ServiceItem, Stat, ProjectItem, Testimonial, CompanyInfo, HeroSection, AboutSection, Lead)
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
  static/projects.html     full projects gallery (with a click-to-enlarge lightbox) -
                            linked from index.html's "View All Projects" once there
                            are more than the homepage's 6-item preview
  static/testimonials.html full testimonials list - same "View All" pattern, 3-item
                            homepage preview
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
| Home / Cover section | `/api/hero-section` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\*, POST/{id}/image\* (upload), GET/{id}/image |
| About Us section | `/api/about-section` | GET, GET/{id}, POST\*, PUT/{id}\*, DELETE/{id}\*, POST/{id}/image\* (upload), GET/{id}/image |
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

Full curl examples with sample requests and responses are in the "BuildPro API
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
  the Home/Cover banner, About Us, services, stats, projects, testimonials, and
  company info. Each section is a table with an "+ Add" button; editing opens a
  small form in a modal, which shows field-specific validation errors (e.g. "title
  is required" under the Title field) instead of only a generic failure message.
  Changes go live immediately once published, since the public page reads the same
  data via `/api/content`.
  - Home/Cover and About Us (like Company info) are singletons: there's always
    exactly one row, so their table only offers Edit — no "+ Add" or Delete.
  - Services, stats, projects, and testimonials support drag-to-reorder: drag a row
    by its handle to change `displayOrder` instead of typing a number. Only the
    rows whose order actually changed are saved (existing `PUT {path}/{id}`, no new
    endpoint); new items are appended to the end automatically.
  - Services, stats, projects, and testimonials also have a **Published**/**Draft**
    flag (`published` on the entity, defaulting to `true` so nothing already live
    goes dark). A draft is saved and fully editable in the admin panel like any
    other item, but is filtered out of `/api/content` — the endpoint the public
    page actually fetches from — so it stays invisible on the live site until you
    flip it back to Published. Toggle it from the checkbox in the edit form, or
    the quick Publish/Unpublish button next to each row. (Home/Cover, About Us,
    and Company Info are singletons with no draft concept — they're either
    configured or they aren't.)
  - Projects, Home/Cover's background image, and About Us's image all have the same
    hybrid image upload option: the Image field takes either a pasted URL or a
    picked file. An uploaded file is sent to `POST {path}/{id}/image` (multipart,
    5MB max) and stored as bytes directly in Postgres, served back via
    `GET {path}/{id}/image` (public, cached a day) — e.g.
    `POST /api/projects/{id}/image`, `POST /api/hero-section/{id}/image`,
    `POST /api/about-section/{id}/image`. Switching the field back to a pasted URL
    (or clearing it) drops the previously uploaded bytes on save. See "Database
    migrations" below.

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
`/api/testimonials`, `/api/company-info`, `/api/hero-section`, and `/api/about-section`
require the admin login — `GET` on all of them stays public, since the live site's
own `/api/content` call depends on it. The `POST {path}/{id}/image` upload
sub-endpoints (projects, hero-section, about-section) each have their own explicit
authenticated rule in `SecurityConfig`, since a base resource's POST rule doesn't
cover a nested sub-path; `GET {path}/{id}/image` stays public alongside every
other `GET`.
A fetch call from the admin pages that isn't signed in (e.g. an expired session)
gets a clean `401` rather than a redirect, so the page can show "not signed in"
instead of a broken response; a direct browser visit to a protected admin page still
redirects to `/admin/login` as expected.

The leads page displays submission times in a configurable timezone
(`app.display-timezone` in `application.yaml`, backed by the `DISPLAY_TIMEZONE` env
var, defaulting to `Asia/Kolkata`) — fetched at page load from `GET /api/config`, not
hardcoded in the page itself. Submission times are stored on the server in UTC (the
JVM's clock on Railway), so this only affects how they're *displayed*.

## Lead notifications

New submissions to the contact form (`POST /api/leads`) can trigger an email so
you don't have to keep checking `/admin/leads` manually — see
`service/impl/LeadNotificationServiceImpl.java`. Off by default; enable it with:

| Env var | Required | Default | Purpose |
| --- | --- | --- | --- |
| `LEAD_NOTIFICATIONS_ENABLED` | to turn it on | `false` | Set to `true` to actually send emails. |
| `LEAD_NOTIFICATION_EMAIL` | no | *(none)* | Recipient override - see "Who receives it" below. |
| `MAIL_USERNAME` | when enabled | *(none)* | The sending account's full email address. |
| `MAIL_PASSWORD` | when enabled | *(none)* | An **app password**, not the account's normal login password (see below for Gmail). |
| `MAIL_HOST` | no | `smtp.gmail.com` | SMTP host. |
| `MAIL_PORT` | no | `587` | SMTP port (STARTTLS). |
| `MAIL_FROM` | no | `MAIL_USERNAME`'s value | Override the `From:` address shown to the recipient. |

**Who receives it:** if `LEAD_NOTIFICATION_EMAIL` is set, that address always
wins. Otherwise it falls back to the **Company Info** section's `email` field —
the same address shown in the site's footer/contact section, editable via
`/admin/content` → Company Info, with no redeploy needed to change it. If
neither is set, notifications are skipped (and logged as such) until one is.

**Setting up a Gmail app password** (simplest option for this volume of email):

1. On the Gmail account that will send the notifications, turn on 2-Step
   Verification if it isn't already (Google Account → Security).
2. Google Account → Security → "2-Step Verification" → "App passwords".
3. Create one (any name, e.g. "BuildPro leads") and copy the 16-character
   password it generates — that's `MAIL_PASSWORD`, not the account's real
   password.
4. Set `MAIL_USERNAME` to the full Gmail address and `LEAD_NOTIFICATIONS_ENABLED=true`.
   Leave `LEAD_NOTIFICATION_EMAIL` unset to have notifications go to whatever
   email is set in Company Info, or set it explicitly to send them somewhere
   different from that public-facing address.

Any other SMTP provider (SendGrid, Resend, Mailgun, etc.) works the same way —
just point `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` at that
provider's SMTP credentials instead.

A failed send (wrong credentials, SMTP provider temporarily down, no recipient
resolved) is only logged server-side — it never fails the contact form
submission itself, since the lead is already saved in Postgres before the
email is attempted. Check the app logs if you enable this and don't see
emails arriving.

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
- `003-hero-about-sections.yaml` - `createTable` for `hero_section` and
  `about_section` (the Home/Cover banner and About Us admin sections),
  seeded with the copy/images that were previously hardcoded in
  `index.html`.
- `004-hero-about-image-upload.yaml` - `addColumn` on `hero_section` and
  `about_section` (`*_image_data` `BYTEA`, `*_image_content_type`
  `VARCHAR(255)`), so both sections support the same upload-a-file image
  option as Projects, instead of only a pasted URL.

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

**Liquibase / Spring Boot circular-dependency note:** the app crashed on
every single startup attempt (both locally and on Railway) with
`BeanCreationException: Circular depends-on relationship between 'liquibase'
and 'entityManagerFactory'`. This turned out to be a genuine Spring Boot 4.0.x
autoconfiguration bug, confirmed reproducible across **4.0.4, 4.0.4 with
`liquibase-core` force-pinned to 4.33.0, and 4.0.8** (the latest 4.0.x patch
as of writing) - ruling out both the Liquibase-core version and the Boot patch
version as the cause before landing on the real one:
`LiquibaseAutoConfiguration` imports `DatabaseInitializationDependencyConfigurer`,
which is meant to make `entityManagerFactory` depend on `liquibase` (JPA
should wait for migrations - correct). For this app's bean combination, Boot
also wires the reverse edge - `liquibase` depending on `entityManagerFactory`,
which has no reason to exist - and Spring's own circular-dependency check then
rejects the resulting cycle outright.

Fixed with a small `BeanFactoryPostProcessor` -
`config/LiquibaseJpaDependsOnFixConfig.java` - that runs before the context
refreshes and strips `entityManagerFactory` out of the `liquibase` bean
definition's `dependsOn` list, leaving `entityManagerFactory`'s own `dependsOn`
(on `liquibase`) untouched. Migrations still run before JPA starts; only the
bad reverse edge is gone, so the cycle no longer exists. If a future Spring
Boot upgrade fixes this properly upstream, this class becomes a harmless
no-op - confirm that (comment it out, verify startup still works) before
deleting it.

## Changelog

See `CHANGELOG.md` for the full history of changes to this project.
