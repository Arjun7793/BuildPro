# buildpro Deployment Guide

2026-09-20 · @Someone

## Overview

buildpro (a Spring Boot 4.0.4 backend for the BuildPro Construction static site) is deployed on [Railway](https://railway.app), chosen for its simplicity and free subdomain support (no custom domain needed for this project). The service runs the `prod` Spring profile against a Railway-managed Postgres database.

Live URL: <https://buildpro-production-bd6d.up.railway.app/>

Getting from a clean GitHub push to a working live deployment took five separate build/runtime failures, each with a distinct root cause. This doc walks through the setup and every issue hit along the way, in the order they occurred, so the same mistakes aren't repeated on a future redeploy or a second project.

## Deployment steps

1. **Connect the repo.** In Railway, created a new project and added the GitHub repo (`Arjun7793/``B``u``i``l``dPro`) as a service — Railway auto-detected it as a Gradle project and used its Railpack builder.
2. **Add a Postgres database.** In the project canvas, clicked **+ New → Database → Add PostgreSQL**. This provisions a service named `Postgres` with connection details exposed as variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, etc.) that other services in the same project can reference.
3. **Set environment variables** on the `buildpro` service (Variables tab → Raw Editor), referencing the Postgres service's variables rather than hardcoding them:

```
SPRING_PROFILES_ACTIVE="prod"
DB_URL="jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}"
DB_USERNAME="${{Postgres.PGUSER}}"
DB_PASSWORD="${{Postgres.PGPASSWORD}}"
```

This initial set doesn't include `SPRING_JPA_HIBERNATE_DDL_AUTO` or `SPRING_SQL_INIT_MODE` — those were added afterward, only for the one-time schema bootstrap in step 5 (see Issues 4 & 5, and the Environment variables table below).

Railway's `${{ServiceName.VAR}}` syntax resolves at deploy time — `ServiceName` must exactly match the target service's name in the canvas (case-sensitive). 4. **Generate a public domain.** Settings → Networking → **Generate Domain**, target port `8080` (matches `server.port: ${PORT:8080}` in `application-prod.yaml`, which respects Railway's injected `PORT` env var automatically). This produces a `*.up.railway.app` URL — separate from the `*.railway.internal` private address also shown, which only works for service-to-service traffic inside the project. 5. **One-time schema + seed bootstrap** (see the environment variables section below) — temporarily let Hibernate create the schema and `data.sql` seed it, then locked both down for all future deploys. 6. **Verify** — `curl` the live `/api/content` endpoint and load the page in a browser (see Verification section).

## Issue 1: Java toolchain download failure

**Error:**

```
Cannot find a Java installation on your machine matching: {languageVersion=25, ...}.
Toolchain download repositories have not been configured.
```

**Cause:** `build.gradle` originally used a Gradle toolchain pinned to Java 25 (`java { toolchain { languageVersion = JavaLanguageVersion.of(25) } } }`). Railway's build container ships JDK 21 only, so Gradle needed to download JDK 25 for the toolchain — and had no configured way to do that.

**First attempt (didn't work):** Added the `org.gradle.toolchains.foojay-resolver-convention` plugin to `settings.gradle`, which lets Gradle auto-download the exact JDK a toolchain requests. Confirmed via `git log`/`git show` that this fix genuinely reached Railway, but the identical error recurred on the next build — pointing to Railway's build sandbox not being able to reach the Foojay JDK-download API at all (a different host than Maven Central, which worked fine for the Gradle wrapper itself).

**Actual fix:** Removed the Gradle toolchain requirement entirely. Replaced it in `build.gradle` with plain `sourceCompatibility`/`targetCompatibility`:

```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

This makes Gradle compile with whatever JDK is already running it (local JDK 25, Railway's JDK 21) — no downloads needed anywhere — while keeping the compiled bytecode at a consistent Java 21 baseline. The now-unused foojay plugin was removed from `settings.gradle`.

## Issue 2: bootJar couldn't resolve the main class

**Error:**

```
> Task :bootJar FAILED
Error while evaluating property 'mainClass' of task ':bootJar'.
  Main class name has not been configured and it could not be resolved from classpath
```

This appeared only after Issue 1 was fixed — `compileJava` and `resolveMainClassName` both succeeded, but packaging the jar failed.

**Cause:** `BuildproApplication.main` was declared `static void main(String[] args)` — missing the `public` modifier, likely lost during the earlier project rename from `SampleStarterApplication` to `BuildproApplication`. Spring Boot's Gradle plugin locates the main class by scanning compiled classes for a `public static void main(String[])` method; a package-private one is invisible to that scan. This wasn't just a build-tool quirk — the JVM itself requires `public static void main` to launch a class, so even a successfully-packaged jar would have failed the same way when run with `java -jar` anywhere, not just on Railway.

**Fix:** Added the missing `public` modifier:

```java
public static void main(String[] args) {
    SpringApplication.run(BuildproApplication.class, args);
}
```

## Issue 3: start command couldn't find the jar

**Symptom:** The build succeeded, but the container crash-looped immediately after start, with the deploy logs showing:

```
ls: cannot access '*/build/libs/*jar': No such file or directory
Usage: java [options] <mainclass> [args...]
```

Railway's dashboard still showed "Deployment successful" (that only reflects the build/deploy pipeline steps, not whether the app actually stays up), while the live URL returned `502 Application failed to respond`.

**Cause:** Railpack's auto-detected start command for Gradle projects assumes the jar lives under a subdirectory (`*/build/libs/*.jar`, e.g. a multi-module Gradle layout). This is a single-module project — the jar sits directly at `build/libs/*.jar` — so the glob never matched anything, and the container just printed `java`'s usage/help text and exited, repeatedly.

**Fix:** Added a `railway.json` at the project root with an explicit start command, plus a restart policy so a transient failure doesn't leave the service down indefinitely:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": { "builder": "RAILPACK" },
  "deploy": {
    "startCommand": "java -jar $(ls build/libs/*.jar | grep -v plain | head -n 1)",
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 3
  }
}
```

The `grep -v plain` excludes Spring Boot's `bootJar`-adjacent `-plain.jar` (a non-executable jar Gradle's plain `jar` task also produces) and picks the real executable jar.

## Issue 4: empty JDBC URL (no Postgres service existed)

**Error:**

```
Driver org.postgresql.Driver claims to not accept jdbcUrl, jdbc:postgresql://:/
```

**Cause:** `DB_URL` referenced `${{Postgres.PGHOST}}`, `${{Postgres.PGPORT}}`, `${{Postgres.PGDATABASE}}`, but no Postgres service existed yet in the Railway project — the reference variables resolved to empty strings rather than erroring, producing a URL with no host, port, or database name.

**Fix:** Added a PostgreSQL service via the project canvas (**+ New → Database → Add PostgreSQL**). Railway names it `Postgres` by default, matching the `${{Postgres.*}}` references already in place. Redeployed the `buildpro` service afterward so it picked up the now-resolvable variables. Note: Railway's `${{ServiceName.VAR}}` syntax is case-sensitive and must match the referenced service's exact name in the canvas — worth double-checking if this happens again with a renamed or differently-named database service.

## Issue 5: data.sql ran before the schema existed

**Error:**

```
Caused by: org.postgresql.util.PSQLException: ERROR: relation "services" does not exist
```

**Cause:** With `SPRING_JPA_HIBERNATE_DDL_AUTO=update` and `SPRING_SQL_INIT_MODE=always` set (to bootstrap schema + seed data on Postgres for the first time), Spring Boot ran `data.sql`'s `INSERT` statements *before* Hibernate created the tables. Locally this doesn't happen because `application-local.yaml` sets `defer-datasource-initialization: true`, which orders schema creation before data scripts — that setting wasn't present in the temporary prod bootstrap env vars.

**Fix:** Added one more environment variable alongside the bootstrap ones:

```
SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION="true"
```

With that in place, the redeploy created all 6 tables (`company_info`, `leads`, `projects`, `services`, `stats`, `testimonials`) and seeded them correctly — confirmed by querying each table directly in Railway's Postgres data browser.

## Environment variables: bootstrap vs. permanent

All set on the `buildpro` service's Variables tab (Raw Editor). Every step used the same connection variables; only the schema/seed-control ones changed between the one-time bootstrap and the permanent steady state.

| Variable | Bootstrap (one-time, to create + seed schema) | Permanent (every deploy after) |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod` |
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` | same |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` | same |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | same |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | `validate` |
| `SPRING_SQL_INIT_MODE` | `always` | `never` |
| `SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION` | `true` | `true` |

The permanent values matter: leaving `update`/`always` in place would let the app silently alter schema or re-run seed inserts on every future restart or redeploy — `validate`/`never` makes Hibernate check the schema matches the entities (failing loudly if it doesn't) without ever touching data automatically.

## Verification

1. **Check deploy logs first**, not just the build/deployment status in the dashboard — "Deployment successful" only confirms the build+deploy pipeline ran, not that the app is staying up (see Issue 3). Look for Spring Boot's startup banner and `Started BuildproApplication` with no stack trace after it.
2. **Hit the combined content endpoint:**

```bash
curl -i https://buildpro-production-bd6d.up.railway.app/api/content
```

Expect `HTTP/2 200` and a JSON body with `services`, `stats`, `projects`, `testimonials`, and `companyInfo` populated. 3. **Load the page in a browser** at the same URL and confirm the static page renders with live data (services grid, stats counters, project images, testimonials, company info, contact form) — not just the empty page shell. 4. **Swagger/OpenAPI is intentionally disabled in `prod`** (`application-prod.yaml` sets `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`) — it's only available when running locally under the `local` profile, at `http://localhost:8080/swagger-ui.html`.
