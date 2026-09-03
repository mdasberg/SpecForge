## Context

The repository contains a design prototype and no code. The stack is therefore an open
decision, and it is the one decision in this plan that is expensive to reverse, so it is
recorded here rather than assumed silently.

Known constraints: the author works in Java/Spring for backend services and has a
Gradle + Spring Boot + React precedent in comparable projects; SpecForge's data model is
relational and heavily audited (immutable versions, append-only events, approval rules); the
UI is dense and already designed as a single-page app.

## Goals / Non-Goals

**Goals:**
- A stack the author can maintain at speed, with a relational store and real migrations.
- Capability boundaries visible in the code and verified automatically, so nine follow-on
  changes can be built in near-isolation.
- The design system enters the codebase once, as tokens, not re-derived per screen.

**Non-Goals:**
- Microservices, or any deployment topology beyond one service plus one database.
- Multi-tenancy beyond a single organisation.
- Kafka, an event bus, or SpiceDB-style fine-grained authorization at this stage.
- Building account management: registration, password reset, invitations and MFA belong to
  Keycloak, and SpecForge does not put a screen in front of them.

## Decisions

- **Java 25 · Spring Boot 4 · Spring Modulith 2, one module per capability.** Modulith gives
  package-level boundaries with a `verifies()` test and in-process events, which is exactly the
  isolation this plan needs without splitting the deployment.
- **MySQL + Liquibase.** Relational fits versions, reviews, verdicts and events. Liquibase over
  Flyway to match the author's other services.
- **Audit is an owned append-only table, not Hibernate Envers.** The audit trail is a product
  feature with its own event shape (actor kind, review context), not a row-history side effect.
- **React 19 + TypeScript + Vite, served separately in dev and as static assets in prod.** The
  designed UI is a dense SPA; server-rendered templates would fight it.
- **Design tokens are copied, not imported.** `design/` is a design canvas with its own build;
  making the app depend on it would couple product code to a prototype. The copy is one file
  and drift is visible in review.
- **Keycloak is the identity provider and the user store.** SpecForge builds no account, password,
  invitation or MFA screens; those exist in Keycloak already and are a liability to reimplement.
  The local `user` table is a mirror of the token subject (display name, avatar, roles, actor kind),
  keyed by the Keycloak subject id, so comments and verdicts can carry a stable foreign key and
  survive a rename in the provider.
- **The backend is a resource server, not an OAuth2 client.** It validates the Keycloak-issued JWT
  through `spring.security.oauth2.resourceserver.jwt.issuer-uri`; the browser does the login. That
  keeps the API stateless and makes the check runners' service accounts work through the same code
  path as human tokens.
- **Two realm clients.** `specforge-web` is public, standard flow with PKCE, redirect URIs and web
  origins for the Vite dev server and the deployed origin — the SPA logs in by redirect, so it needs
  the standard flow rather than direct access grants. `specforge-api` is confidential with service
  accounts enabled, which is how `add-automated-review`'s check runners authenticate as their agent
  identity. Direct access grants stay enabled in the development realm only, for fetching a token
  in tests.
- **Roles are Keycloak realm roles, mapped one-to-one.** `REVIEWER`, `ARCHITECT`, `ADMIN` come from
  the token's realm roles; nothing else grants a role, and a role change in Keycloak takes effect on
  the next token. Approval rules need "is an architect", nothing finer yet. Project membership is
  SpecForge's own data, not a Keycloak group, because it changes with repositories rather than with
  people.
- **The dev realm ships in the repository.** `keycloak/realm-export.json` is imported on container
  start with a seeded user per role, so a clean checkout can log in without console clicks. The
  export holds development credentials only and is not the deployment configuration.
- **Package base `com.specforge`.**

## Risks / Trade-offs

- **The stack itself is an assumption.** If SpecForge is meant to be a standalone product with a
  TypeScript-only team, a Node/TS backend would be the better call, and this change is the only
  one that would need rewriting — the nine capability changes are stack-agnostic in their
  requirements. Decide before starting task group 1.
- Copied design tokens can drift from `design/parts/shell.css`. Accepted: a mismatch is a
  cosmetic bug, and a build-time dependency on the prototype is worse.
- A role check will not express "the owning team's architect". When that requirement appears,
  swapping in policy-based authorization is a contained change behind the approval-rule
  evaluator.
- Keycloak is a second container to run and a second thing to operate in production. Accepted: the
  alternative is building the account surface SpecForge explicitly does not want to own.
- The mirrored `user` table can drift from Keycloak (a display name changed in the console shows
  the old value until that user's next token). Accepted: it refreshes on every login, and the
  subject id — not the name — is the key everything else references.
- Committing a realm export with development credentials is fine only as long as it stays a
  development realm. The deployment realm is configured outside this repository, and the export
  says so at the top.
