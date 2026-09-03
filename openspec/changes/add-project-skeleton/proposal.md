## Why

The SpecForge product is fully designed (clickable prototype in `design/`) but the repository
holds no application code. Every later capability — repository ingestion, review, approval,
agent checks, audit — needs the same foundation: a runnable service with a schema, an
authenticated user with a role, and an application shell that already carries the design
system. Building that foundation once, as its own change, keeps the nine capability changes
that follow about behaviour instead of plumbing.

## What Changes

- Introduce a single Spring Boot application structured as a Spring Modulith modular monolith,
  one module per capability, with module boundaries enforced by a test rather than by review.
- Introduce the MySQL schema with Liquibase migrations, plus a docker-compose for local MySQL
  and Keycloak.
- Introduce Keycloak as the identity provider and the place users, groups and credentials are
  managed: a `specforge` realm imported from `keycloak/realm-export.json`, a public web client
  using authorization code with PKCE, a confidential API client whose service accounts later
  authenticate the check runners, and the three realm roles `REVIEWER`, `ARCHITECT` and `ADMIN`.
- Validate Keycloak-issued access tokens in the backend as a resource server, mirror the token's
  identity and roles into a local identity record, and refuse unauthenticated API access.
- Introduce the React + TypeScript frontend shell: primary navigation Home / Specs / Reviews /
  Projects / Activity, dark theme by default plus light, tokens lifted verbatim from
  `design/parts/shell.css`.
- Fix the API conventions the other changes build on: JSON REST under `/api`, RFC 9457
  `application/problem+json` errors, a single pagination envelope.
- No product behaviour ships in this change — the screens are empty states.

## Capabilities

### New Capabilities
- `project-skeleton`: the runnable application, its module boundaries, authentication, roles,
  API conventions and the themed application shell.

### Modified Capabilities
(none — first change in the repository)

## Impact

- New Gradle build, `com.specforge` package base, one Modulith module per planned capability
  (initially empty apart from `platform`).
- New `frontend/` Vite app; the design system is copied from `design/parts/shell.css` into
  `frontend/src/styles/tokens.css` — the prototype stays the reference, not a build dependency.
- New `keycloak/realm-export.json` and a Keycloak service in `docker-compose.yml`:
  `quay.io/keycloak/keycloak:26.0`, `start-dev --import-realm`, the realm JSON mounted read-only at
  `/opt/keycloak/data/import/realm-export.json`, published on host port 8081, with a healthcheck
  against `/realms/specforge` and admin credentials from the environment.
- Local development needs Docker for MySQL and Keycloak. No user administration screens are built:
  accounts, group membership, password and MFA policy stay in Keycloak's own console.
- Depends on: nothing. Everything else depends on this.
