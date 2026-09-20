# Postgres Local Dev Setup

2026-09-20 · @Someone

## Overview

This records how the local Postgres database for the `sample_starter` Spring Boot project was set up on Arjun's Mac, and every issue hit along the way, in the order they came up. The end state: Postgres 16 running via Homebrew, a `sample_starter` database, password authentication enforced, and DBeaver connected to it.

## Issue 1 — Docker wasn't installed

The original plan was to run Postgres in a Docker container. Running `docker run ...` failed with `zsh: command not found: docker` — Docker Desktop wasn't installed. Rather than install a GUI app, the setup switched to a native Postgres install via Homebrew instead.

## Issue 2 — Homebrew wasn't installed either

`brew --version` returned `zsh: command not found: brew`. Homebrew had to be installed first:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

This prompts for the Mac login password partway through. After it finishes, it prints a couple of lines to add `brew` to the shell's PATH — on Apple Silicon these are:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

(On an Intel Mac the path is `/usr/local/bin/brew` instead.) Confirmed with `brew --version`.

## Installing Postgres and creating the database

```bash
brew install postgresql@16
brew services start postgresql@16
```

Then create the `postgres` superuser role, set its password, and create the `sample_starter` database:

```bash
/opt/homebrew/opt/postgresql@16/bin/createuser -s postgres
/opt/homebrew/opt/postgresql@16/bin/psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'Hertzberger7793';"
/opt/homebrew/opt/postgresql@16/bin/createdb -U postgres sample_starter
```

(Intel Macs: swap `/opt/homebrew/opt/postgresql@16` for `/usr/local/opt/postgresql@16`.)

Sanity check that it's reachable over the network (not just the Unix socket) with password auth:

```bash
/opt/homebrew/opt/postgresql@16/bin/psql -U postgres -d sample_starter -h localhost
```

## DBeaver connection

Database → New Database Connection → PostgreSQL, then:

| Field | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `sample_starter` |
| Username | `postgres` |
| Password | `Hertzberger7793` |

DBeaver may prompt to download the PostgreSQL JDBC driver the first time — accept it. "Test Connection…" confirmed success, and the `sample_starter` database showed up under the `postgres` connection alongside its Schemas, Roles, and Extensions.

## Issue 3 — DBeaver connected with no password entered

DBeaver connected successfully even with the password field left blank. This wasn't a bug: Homebrew's default `pg_hba.conf` (the file controlling authentication rules) sets local connections — both the Unix socket and `127.0.0.1`/`::1` — to `trust` authentication:

```
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
```

Under `trust`, Postgres accepts any username from a local connection without checking a password at all. It's meant to make local development frictionless, not for production use.

## Fix — requiring a password locally

Find and edit the `pg_hba.conf` file:

```bash
nano "$(/opt/homebrew/opt/postgresql@16/bin/psql -U postgres -d postgres -tAc 'SHOW hba_file;' | tr -d '[:space:]')"
```

Change the three non-replication lines from `trust` to `scram-sha-256`:

```
local   all             all                                     scram-sha-256
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
```

(The `replication` lines were left as `trust` — not relevant to this app.) Save, then restart Postgres:

```bash
brew services restart postgresql@16
```

After this, `psql -U postgres -d sample_starter -h localhost` correctly prompts for the password, and DBeaver's saved connection keeps working because it already has the password stored. One side effect: local commands like `createdb`/`createuser` run without `-h localhost` now go through the Unix socket, which also asks for a password after this change.

## Spring Boot project config

`src/main/resources/application-local.yaml` now points at this database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sample_starter
    username: postgres
    password: Hertzberger7793
    driver-class-name: org.postgresql.Driver
```

and `build.gradle` gained the JDBC starter and Postgres driver:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-jdbc'
runtimeOnly 'org.postgresql:postgresql'
```

`application-prod.yaml` keeps its datasource block commented out with env-var placeholders (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) until a real prod database exists.

## Credentials reference

| Setting | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `sample_starter` |
| Username | `postgres` |
| Password | `Hertzberger7793` |
| Auth method | `scram-sha-256` (was `trust` by default) |
| Service | `brew services` → `postgresql@16` |
