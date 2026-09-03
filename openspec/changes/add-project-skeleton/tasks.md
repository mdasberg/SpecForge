# Tasks: add-project-skeleton

## 1. Build and module skeleton

- [x] 1.1 Gradle multi-source Spring Boot 4 application on Java 25, package base `com.specforge`.
- [x] 1.2 Spring Modulith 2 with one (initially empty) module package per planned capability:
      `platform`, `repository`, `catalog`, `review`, `discussion`, `approval`, `agent`,
      `ticket`, `audit`, `dashboard`.
- [x] 1.3 `ModularityTests` asserting `ApplicationModules.of(...).verify()`, wired into `check`.
- [x] 1.4 Modulith documentation task so the module graph is generated, not drawn by hand.

## 2. Persistence and local environment

- [x] 2.1 PostgreSQL datasource plus Liquibase master changelog (XML) with a per-module
      changelog include. Spring Boot 4 splits auto-configuration per technology, so `spring-boot-liquibase`
      is a required runtime dependency alongside `liquibase-core` — without it the migrations
      silently never run.
- [x] 2.2 `compose.yml` with PostgreSQL and Keycloak, plus a README quickstart:

      ```yaml
      keycloak:
        image: quay.io/keycloak/keycloak:26.0
        container_name: specforge-keycloak
        command: start-dev --import-realm
        environment:
          # Keycloak 26 reads the first-boot admin from KC_BOOTSTRAP_ADMIN_*; KEYCLOAK_ADMIN_* are
          # deprecated. The KEYCLOAK_ADMIN/_PASSWORD names stay as the host-side knobs.
          KC_BOOTSTRAP_ADMIN_USERNAME: ${KEYCLOAK_ADMIN:-admin}
          KC_BOOTSTRAP_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:-admin}
        ports:
          - "127.0.0.1:8081:8080"
        volumes:
          - './keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro'
        healthcheck:
          test: ["CMD-SHELL", "exec 3<>/dev/tcp/localhost/8080 && echo -e 'GET /realms/specforge HTTP/1.1\\r\\nhost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q 'realm' <&3"]
          interval: 5s
          timeout: 5s
          retries: 20
          start_period: 30s
      ```

- [x] 2.3 Integration tests run against the `compose.yml` environment — real PostgreSQL, no H2, no
      Testcontainers: a docker-compose Gradle plugin brings the environment up for an
      `integrationTest` task, which runs a dedicated `itest` source set at `src/itest/java` (the
      layout the CarePay services use, so the source set is the filter and its classes are named
      `*Test`, not `*IT`), and `BaseIntegrationTest` carries the shared Spring context and the
      datasource pointed at the composed PostgreSQL. The stack is left running afterwards rather than torn
      down, matching the CarePay `java-docker-compose-conventions` plugin this is modelled on: the
      developer keeps the environment they already had, and a CI runner discards it with the job.
      `projectName` is left null so the plugin drives the identical compose project a bare
      `docker compose up` in this directory creates.

## 3. Keycloak realm

- [x] 3.1 `keycloak/realm-export.json` for realm `specforge`: `enabled: true`, an
      `accessTokenLifespan` of 3600 for development, the realm roles `REVIEWER`, `ARCHITECT` and
      `ADMIN`, and a `description` stating it is the development realm only.
- [x] 3.2 Client `specforge-web`: `publicClient: true`, `standardFlowEnabled: true` (PKCE
      `S256` required), `redirectUris` and `webOrigins` for `http://localhost:5173` and the deployed
      origin, `directAccessGrantsEnabled: true` for tests only.
- [x] 3.3 Client `specforge-api`: `publicClient: false` with a secret, `serviceAccountsEnabled: true`,
      `standardFlowEnabled: false` — the credential the check runners in `add-automated-review`
      authenticate with.
- [x] 3.4 Seed one development user per role with a non-temporary password credential and empty
      `requiredActions`, so a clean checkout can log in without touching the Keycloak console.
- [x] 3.5 Realm role mapper on both clients so realm roles land in the token where the resource server
      reads them (`realm_access.roles`).
- [x] 3.6 Document the deployment realm's differences in the README: real credentials from the
      environment, no seeded users, no direct access grants, a real token lifespan.

## 4. Authentication and identity mirroring

- [x] 4.1 Resource-server JWT validation:

      ```yaml
      spring:
        security:
          oauth2:
            resourceserver:
              jwt:
                issuer-uri: http://localhost:8081/realms/specforge
      ```

      with the issuer overridden per environment and never defaulted to a production realm.
- [x] 4.2 Frontend login through `specforge-web` with authorization code plus PKCE, token refresh, and
      logout that ends the Keycloak session. Built on `oidc-client-ts` / `react-oidc-context` rather
      than hand-rolled: PKCE, state and silent renew are security-critical and not worth
      reimplementing. **Not browser-verified** — the redirect flow itself has no automated test; the
      token path it produces is covered end to end by the integration suite.
- [x] 4.3 Map the token's realm roles to `REVIEWER` / `ARCHITECT` / `ADMIN` as granted authorities;
      default `REVIEWER`; ignore unknown realm roles.
- [x] 4.4 Persist a `user` record keyed by the Keycloak subject id (display name, avatar, roles,
      `actor_kind = HUMAN`) on first authenticated request, refreshing name, avatar and roles from each
      token — the same table later carries agent identities with `actor_kind = AGENT`.
- [x] 4.5 `/api/me` returning the mirrored identity and its roles.
- [x] 4.6 Refuse unauthenticated `/api/**` with 401 problem+json, and a request whose token lacks the
      required role with 403 problem+json.
- [x] 4.7 No account-management endpoints or screens: point the user at Keycloak for profile, password
      and MFA.

## 5. API conventions

- [x] 5.1 `@ControllerAdvice` rendering every error as RFC 9457 problem+json, including validation failures.
- [x] 5.2 Shared pagination envelope (`items`, `total`, `cursor`) and a shared sort/filter parameter parser.
- [x] 5.3 The API is contract-first. `src/main/resources/openapi/specforge-api.yaml` is the
      contract; the `org.openapi.generator` Gradle plugin generates the server interfaces and the
      request and response types from it, `compileJava` depends on that task, and every controller
      implements a generated interface so drift fails the build. No annotation-scanning document
      generator: the code does not describe the API, the contract does.
- [x] 5.5 The contract is split the way the other CarePay services split theirs, so a capability
      change edits its own file instead of one file every change has to touch: one file per path
      under `resources/<area>/`, one per schema under `schemas/`, and the shared parameters and
      responses under `parameters/` and `responses/`. The root file carries `info`, `servers`,
      `security`, `tags`, the `paths` map of `$ref`s, and the shared `securitySchemes` and
      `parameters`. Shared responses are **not** listed in the root: a resource references
      `responses/<Name>.yaml` directly, because listing them in both places leaves the bundled
      document carrying a `$ref` to a file that does not exist at the URL it is served from.
- [x] 5.4 The contract is served at `/api/openapi.json` — the same document the interfaces were
      generated from, bundled to JSON at build time, not a description rebuilt from the
      implementation. It sits under `/api/**`, so it needs a token like every other route rather
      than describing the API surface to anonymous callers.

## 6. Frontend shell

- [x] 6.1 Vite + React 19 + TypeScript app with routing for Home / Specs / Reviews / Projects / Activity.
- [x] 6.2 Copy the design system from `design/parts/shell.css` into `frontend/src/styles/tokens.css`;
      port topbar, nav, side panel, badges, buttons and avatars as components. The source was
      recovered from the `plan/openspec-implementation` branch, where `design/` is committed —
      CLAUDE.md's claim that it was never committed is wrong. Copies are verbatim so drift shows as
      a diff.
- [x] 6.3 Dark default, light theme toggle, choice persisted per browser; both themes verified against
      the prototype.
- [x] 6.4 Empty state per screen ("no specifications yet — connect a repository"), pointing at the flow
      that `add-spec-repository` delivers.
- [x] 6.5 Authenticated fetch wrapper that attaches the access token, renders problem+json errors, and
      re-authenticates on 401 rather than redirecting to a dead end.

## 7. Verification

- [x] 7.1 Smoke test: application boots, migrations apply, `/api/me` is 401 anonymous and 200 with a
      Keycloak-issued token.
- [x] 7.2 Test: a token carrying the `ARCHITECT` realm role yields the `ARCHITECT` authority, and a
      role removed in Keycloak is gone from the mirrored identity after the next token.
- [x] 7.3 CI runs `build` (unit + Modulith + `integrationTest` against the composed PostgreSQL and
      Keycloak) and the frontend build and typecheck; the realm export is validated by the
      composed Keycloak importing it and reaching its healthcheck.
