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

## Connecting a repository that holds specifications

There is no wizard yet, so a connection is made over the API. The flow is scripted in
`http/specforge.http` — open it in IntelliJ, pick the `dev` environment and run the requests in
order. Everything below is the part that has to happen outside the application first.

### A GitHub App is required

SpecForge reads through a GitHub App installation, never a personal access token: access is scoped
per repository, the account controls revocation, and a revoked installation degrades its
connections rather than deleting what was imported. Without `SPECFORGE_GITHUB_APP_ID` and
`SPECFORGE_GITHUB_PRIVATE_KEY` the forge client refuses every call rather than pretending a
repository is unreachable.

Create one under **Settings → Developer settings → GitHub Apps → New GitHub App**.

**Repository permissions** — these are exactly what the code calls, and nothing more:

| Permission | Access | Why |
|---|---|---|
| Metadata | Read-only | Mandatory for every app |
| Contents | Read-only | The file tree, file contents, and the last commit per path |
| Pull requests | Read-only | Which files a pull request touched |
| Commit statuses | Read **and write** | The one outbound write: a review's state reported back onto the pull request |

**Subscribe to events**: Installation, Installation repositories, Push, Pull request. Anything else
is ignored.

**Webhook URL**: your tunnel's address plus `/api/webhooks/github` (see below). **Webhook secret**:
generate one; every delivery is HMAC-verified against it, and an empty secret rejects everything —
an unverified delivery would be an unauthenticated trigger for imports and outbound writes.

Then **Generate a private key** and keep the `.pem` it downloads.

### The webhook has to reach your machine

An installation is only ever created by the `installation` webhook. No endpoint creates one, so a
local instance with no inbound webhook will show an empty installation list forever. Tunnel it:

```
npx smee-client --url https://smee.io/<your-channel> --path /api/webhooks/github --port 8080
```

Use that `smee.io` URL as the app's webhook URL. Install the app on the repository
(**Only select repositories**), and the delivery that follows registers the installation.

### Run with the app configured

```
export SPECFORGE_GITHUB_APP_ID=<the app id>
export SPECFORGE_GITHUB_PRIVATE_KEY="$(cat /path/to/app.private-key.pem)"
export SPECFORGE_GITHUB_WEBHOOK_SECRET=<the webhook secret>
./gradlew bootRun
```

`GET /api/forge/installations` should now list the installation and the repositories it grants.
Only those repositories can be connected.

### Then scan, then connect

Scan first: it reports what would import, what is a change proposal rather than a specification,
and what cannot be parsed — before anything is created. Then connect, which also triggers the
initial import.

**Get the path glob right.** It is the one setting with no safe default: `openspec/specs/**/spec.md`
is the OpenSpec convention, but a repository that keeps its specifications anywhere else matches
nothing, and a connection that matches nothing importable is refused with a 422 rather than created
empty. The glob is a `java.nio` glob, so `**` crosses directories.

The domain each specification lands in is the path segment directly under the `specs` directory, so
`openspec/specs/billing/spec.md` is domain `billing` and `openspecs/specs/clm/claim/spec.md` is
domain `clm`.

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

## The API is contract-first

`src/main/resources/openapi/specforge-api.yaml` is the source of truth for the HTTP API. The
`org.openapi.generator` plugin generates the server interfaces and the request and response types
from it into `build/generated`, and every controller implements a generated interface — so an
endpoint that no longer matches the contract fails to compile rather than being caught in review.

Change an endpoint by editing the contract first, then the implementation. Never edit the generated
Java: it is regenerated on every build. `/api/openapi.json` serves that same contract, bundled to
JSON at build time, rather than a description rebuilt from the running controllers.

The document is split so that a capability change edits its own file rather than one file every
change has to touch:

```
src/main/resources/openapi/
  specforge-api.yaml          info, servers, security, tags, the paths map, shared components
  resources/<area>/<path>.yaml  one file per path, holding its operations
  schemas/<Name>.yaml         one file per schema
  parameters/<Name>.yaml      parameters shared across endpoints
  responses/<Name>.yaml       responses shared across endpoints
```

A resource references a shared response as `../../responses/<Name>.yaml` directly. Shared responses
are deliberately not also listed under the root's `components.responses`: doing both leaves the
bundled document carrying a `$ref` to a file that does not exist at the URL it is served from.

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
