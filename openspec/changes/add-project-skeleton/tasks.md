## 1. Build and module skeleton

- [ ] 1.1 Gradle multi-source Spring Boot 4 application on Java 25, package base `com.specforge`.
- [ ] 1.2 Spring Modulith 2 with one (initially empty) module package per planned capability:
      `platform`, `repository`, `catalog`, `review`, `discussion`, `approval`, `agent`,
      `ticket`, `audit`, `dashboard`.
- [ ] 1.3 `ModularityTests` asserting `ApplicationModules.of(...).verify()`, wired into `check`.
- [ ] 1.4 Modulith documentation task so the module graph is generated, not drawn by hand.

## 2. Persistence and local environment

- [ ] 2.1 MySQL datasource plus Liquibase master changelog with a per-module changelog include.
- [ ] 2.2 `docker-compose.yml` with MySQL and Keycloak, plus a README quickstart:

      ```yaml
      keycloak:
        image: quay.io/keycloak/keycloak:26.0
        container_name: specforge-keycloak
        command: start-dev --import-realm
        environment:
          KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN:-admin}
          KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:-admin}
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

- [ ] 2.3 Testcontainers-backed integration test base class (real MySQL, no H2).

## 3. Keycloak realm

- [ ] 3.1 `keycloak/realm-export.json` for realm `specforge`: `enabled: true`, an
      `accessTokenLifespan` of 3600 for development, the realm roles `REVIEWER`, `ARCHITECT` and
      `ADMIN`, and a `description` stating it is the development realm only.
- [ ] 3.2 Client `specforge-web`: `publicClient: true`, `standardFlowEnabled: true` (PKCE
      `S256` required), `redirectUris` and `webOrigins` for `http://localhost:5173` and the deployed
      origin, `directAccessGrantsEnabled: true` for tests only.
- [ ] 3.3 Client `specforge-api`: `publicClient: false` with a secret, `serviceAccountsEnabled: true`,
      `standardFlowEnabled: false` — the credential the check runners in `add-automated-review`
      authenticate with.
- [ ] 3.4 Seed one development user per role with a non-temporary password credential and empty
      `requiredActions`, so a clean checkout can log in without touching the Keycloak console.
- [ ] 3.5 Realm role mapper on both clients so realm roles land in the token where the resource server
      reads them (`realm_access.roles`).
- [ ] 3.6 Document the deployment realm's differences in the README: real credentials from the
      environment, no seeded users, no direct access grants, a real token lifespan.

## 4. Authentication and identity mirroring

- [ ] 4.1 Resource-server JWT validation:

      ```yaml
      spring:
        security:
          oauth2:
            resourceserver:
              jwt:
                issuer-uri: http://localhost:8081/realms/specforge
      ```

      with the issuer overridden per environment and never defaulted to a production realm.
- [ ] 4.2 Frontend login through `specforge-web` with authorization code plus PKCE, token refresh, and
      logout that ends the Keycloak session.
- [ ] 4.3 Map the token's realm roles to `REVIEWER` / `ARCHITECT` / `ADMIN` as granted authorities;
      default `REVIEWER`; ignore unknown realm roles.
- [ ] 4.4 Persist a `user` record keyed by the Keycloak subject id (display name, avatar, roles,
      `actor_kind = HUMAN`) on first authenticated request, refreshing name, avatar and roles from each
      token — the same table later carries agent identities with `actor_kind = AGENT`.
- [ ] 4.5 `/api/me` returning the mirrored identity and its roles.
- [ ] 4.6 Refuse unauthenticated `/api/**` with 401 problem+json, and a request whose token lacks the
      required role with 403 problem+json.
- [ ] 4.7 No account-management endpoints or screens: point the user at Keycloak for profile, password
      and MFA.

## 5. API conventions

- [ ] 5.1 `@ControllerAdvice` rendering every error as RFC 9457 problem+json, including validation failures.
- [ ] 5.2 Shared pagination envelope (`items`, `total`, `cursor`) and a shared sort/filter parameter parser.
- [ ] 5.3 OpenAPI document generated from controllers and served at `/api/openapi.json`.

## 6. Frontend shell

- [ ] 6.1 Vite + React 19 + TypeScript app with routing for Home / Specs / Reviews / Projects / Activity.
- [ ] 6.2 Copy the design system from `design/parts/shell.css` into `frontend/src/styles/tokens.css`;
      port topbar, nav, side panel, badges, buttons and avatars as components.
- [ ] 6.3 Dark default, light theme toggle, choice persisted per browser; both themes verified against
      the prototype.
- [ ] 6.4 Empty state per screen ("no specifications yet — connect a repository"), pointing at the flow
      that `add-spec-repository` delivers.
- [ ] 6.5 Authenticated fetch wrapper that attaches the access token, renders problem+json errors, and
      re-authenticates on 401 rather than redirecting to a dead end.

## 7. Verification

- [ ] 7.1 Smoke test: application boots, migrations apply, `/api/me` is 401 anonymous and 200 with a
      Keycloak-issued token.
- [ ] 7.2 Test: a token carrying the `ARCHITECT` realm role yields the `ARCHITECT` authority, and a
      role removed in Keycloak is gone from the mirrored identity after the next token.
- [ ] 7.3 CI runs `build` (unit + Modulith + Testcontainers) and the frontend build and typecheck;
      the realm export is validated by importing it in a Keycloak Testcontainer.
