# SpecForge

SpecForge is a collaborative review and approval platform for software specifications — the
review layer that spec-driven development otherwise lacks. A spec moves through a fixed
lifecycle: Draft, In Review, Changes Requested, Approved, Implemented, with discussions anchored
to the spec text and a traceability chain from spec to approval to ticket to pull request to
deployment.

Only the project skeleton exists so far. The screens that exist are empty states; no product
behaviour has shipped yet.

## Stack

- Java 25
- Spring Boot 4.1.1
- Spring Modulith 2.1.1, one module per capability
- PostgreSQL, with Liquibase managing the schema
- Keycloak for identity
- React 19 + TypeScript + Vite for the frontend (not built yet)

Package base: `com.specforge`.

## Prerequisites

- Docker, to run PostgreSQL and Keycloak locally
- A JDK 25 install

The Gradle wrapper (`./gradlew`) builds with a Java 25 toolchain. On most setups that means
pointing `JAVA_HOME` at a JDK 25 install (for example, one managed by SDKMAN) before running any
`./gradlew` command.

## Quickstart

```
docker compose up -d --wait
./gradlew bootRun
```

`--wait` blocks until both the PostgreSQL and Keycloak healthchecks pass. Keycloak imports the
development realm on its first start, from the mounted `keycloak/realm-export.json`.

To stop:

```
docker compose down       # stop containers, keep the PostgreSQL volume
docker compose down -v    # stop containers and drop the PostgreSQL volume
```

| Service | Address |
|---|---|
| SpecForge application | http://localhost:8080 |
| PostgreSQL | 127.0.0.1:30301 (database `specforge`) |
| Keycloak | http://localhost:8081 |
| Keycloak admin console | http://localhost:8081/admin |

Every port above is published to `127.0.0.1` only, not to the network.

## Development realm and its seeded users

`keycloak/realm-export.json` is imported automatically by the Keycloak container on first start,
so a clean checkout can log in without touching the Keycloak console.

| Username | Password | Realm roles |
|---|---|---|
| reviewer | reviewer | REVIEWER |
| architect | architect | REVIEWER, ARCHITECT |
| admin-user | admin-user | REVIEWER, ADMIN |

The Keycloak admin user itself comes from the `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`
environment variables and defaults to `admin` / `admin`.

Two clients are configured in the realm:

- `specforge-web` — the public SPA client, using authorization code flow with PKCE. It also has
  direct access grants enabled, which is a development-only convenience for fetching a token
  directly in tests.
- `specforge-api` — the confidential client whose service accounts automated check runners
  authenticate as.

## The deployment realm is different

`keycloak/realm-export.json` is the development realm only — it is not the deployment
configuration. A deployed realm differs in these specific ways: real credentials supplied from
the environment rather than committed to the repository, no seeded users, direct access grants
disabled, a real (short) access token lifespan rather than the development realm's
`accessTokenLifespan` of 3600 seconds, and the resource server's `issuer-uri` pointed explicitly
at that realm — never defaulted to a production realm.

SpecForge also builds no account, password, profile or MFA screens of its own. Those live in
Keycloak, and a user who needs them is sent there.

## Build and test

- `./gradlew test` — the unit suite in `src/test`, plus the Spring Modulith boundary
  verification (`ModularityTests`). Needs no Docker.
- `./gradlew integrationTest` — the integration suite in `src/itest`. Brings the `compose.yml`
  stack up itself, through the docker-compose Gradle plugin, and leaves it running afterwards, so
  a stack you already had keeps running. Use `docker compose down` to stop it.
- `./gradlew build` — runs both.
- `./gradlew moduleDocs` — generates the module graph into `build/spring-modulith-docs`.

Integration tests live in their own `itest` source set at `src/itest/java`, matching the CarePay
services. The source set is the filter, so an integration test is named `*Test` like any other and
needs no `*IT` suffix; `BaseIntegrationTest` carries the shared Spring context. Classes and
fixtures from `src/test` are on its classpath.

Integration tests run against the same composed PostgreSQL used for local development rather than
Testcontainers, so the developer, the test task and CI all run against one environment
definition.

## Module layout

Ten Spring Modulith modules live under `com.specforge`:

- `platform` — API conventions, error rendering, pagination, authentication and the mirrored
  identity. Shared by every other module.
- `repository` — connecting a GitHub repository, importing specs, versions and sync.
- `catalog` — browsing, filtering, searching and rendering specifications.
- `review` — reviews, version diff, inline and side-by-side rendering.
- `discussion` — anchored comment threads, mentions and resolution.
- `approval` — reviewers, verdicts, approval rules and the approval gate.
- `agent` — check runs, agent findings, accept and dismiss.
- `ticket` — creating a ticket from a comment and the tracker adapters.
- `audit` — the append-only event log, the timeline and the traceability chain.
- `dashboard` — personal review queues and the activity feed.

`platform` is declared the shared module. A reference across any other module boundary fails
`ModularityTests`, and therefore the build.
