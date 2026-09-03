# project-skeleton

## Purpose

The foundation every SpecForge capability is built on: a runnable modular monolith with enforced
module boundaries, a schema owned by migrations, Keycloak as the identity provider and user store,
an identity mirrored from the token, and one set of API and interface conventions. It carries no
product behaviour of its own — its requirements are the ones every later capability inherits rather
than restates.

## Requirements

### Requirement: Runnable modular monolith

The system SHALL be a single deployable Spring Boot application whose capability modules are
Spring Modulith modules with explicit, automatically verified boundaries, backed by one PostgreSQL
schema whose migrations are applied by Liquibase at startup.

#### Scenario: Fresh checkout starts

- **WHEN** a developer runs the documented quickstart on a clean checkout with Docker available
- **THEN** PostgreSQL and the OIDC provider start, Liquibase applies every migration, and the
  application reports healthy

#### Scenario: Module boundary violation fails the build

- **WHEN** code in one capability module references an internal package of another capability module
- **THEN** the modularity verification test fails and the build does not pass

#### Scenario: Migrations are forward-only

- **WHEN** an already-applied Liquibase changeset is modified
- **THEN** startup fails with a checksum error rather than silently diverging from the schema

### Requirement: Keycloak is the identity provider and the user store

The system SHALL authenticate users against a Keycloak realm, SHALL validate Keycloak-issued access
tokens as a resource server, and SHALL manage no accounts, credentials, group memberships or
multi-factor settings of its own, directing a user who needs those to Keycloak. The realm SHALL be
importable from a development realm export held in the repository so that a clean checkout can be
logged into without manual provider configuration.

#### Scenario: Login goes to Keycloak

- **WHEN** an unauthenticated user opens the application
- **THEN** they authenticate against the Keycloak realm using the authorization code flow with PKCE,
  and return with an access token the backend accepts

#### Scenario: Token from another issuer is refused

- **WHEN** a request presents a token issued by an issuer other than the configured realm
- **THEN** the request is refused with 401 and no identity is created

#### Scenario: No account management in SpecForge

- **WHEN** a user looks for password, profile or multi-factor settings
- **THEN** SpecForge exposes no such endpoint or screen and points them at Keycloak

#### Scenario: Clean checkout can log in

- **WHEN** a developer starts the documented local environment on a clean checkout
- **THEN** the realm is imported with a development user per role, and logging in as one of them succeeds

### Requirement: Identity mirrored from the token, with roles from realm roles

The system SHALL persist, keyed by the Keycloak subject identifier, an identity carrying a display
name, an avatar, an actor kind of `HUMAN` or `AGENT`, and the roles `REVIEWER`, `ARCHITECT` or
`ADMIN` taken from the token's realm roles; SHALL refresh the mirrored name, avatar and roles from
each token presented; SHALL grant no role from any other source; and SHALL refuse requests to
`/api/**` that carry no valid token.

#### Scenario: First authenticated request creates the identity

- **WHEN** a user's token reaches the API for the first time
- **THEN** an identity is persisted against that subject identifier with actor kind `HUMAN`, the
  token's display name and avatar, and at least the `REVIEWER` role

#### Scenario: Architect role comes from the realm

- **WHEN** a user's token carries the `ARCHITECT` realm role
- **THEN** the identity holds the `ARCHITECT` role, which later approval rules can require

#### Scenario: A role removed in Keycloak is removed here

- **WHEN** the `ARCHITECT` realm role is revoked in Keycloak and that user presents a new token
- **THEN** the mirrored identity no longer holds `ARCHITECT`

#### Scenario: A renamed user keeps their history

- **WHEN** a user's display name changes in Keycloak and they present a new token
- **THEN** the mirrored identity shows the new name and every comment, verdict and event previously
  recorded against that subject identifier still resolves to them

#### Scenario: Unknown realm roles are ignored

- **WHEN** a token carries a realm role SpecForge does not define
- **THEN** it is ignored and the identity holds only the defined roles it was granted

#### Scenario: Anonymous API access is refused

- **WHEN** an unauthenticated request is made to any `/api/**` endpoint
- **THEN** the response is 401 with an `application/problem+json` body and no domain data

#### Scenario: Insufficient role is refused

- **WHEN** an authenticated user without the `ADMIN` role calls an administrative endpoint
- **THEN** the response is 403 with an `application/problem+json` body

### Requirement: Service accounts for non-human actors

The system SHALL accept tokens issued to Keycloak service accounts as agent identities with actor
kind `AGENT`, so that automated check runners authenticate through the same token validation as
humans, and SHALL keep those identities in the same identity store as human users.

#### Scenario: Service account authenticates as an agent

- **WHEN** a check runner presents a token obtained through the client credentials grant
- **THEN** the request is authenticated and its identity is persisted or resolved with actor kind `AGENT`

#### Scenario: Agent identity is not a human user

- **WHEN** an agent identity is read
- **THEN** its actor kind is `AGENT`, and every surface that renders authorship can distinguish it
  from a human without inspecting the name

### Requirement: Consistent API envelope and errors

The system SHALL expose its HTTP API under `/api` as JSON, report every error as RFC 9457
`application/problem+json` with a stable `type`, and return every collection through one
pagination envelope carrying `items`, `total` and an optional `cursor`.

#### Scenario: Validation failure is a problem document

- **WHEN** a request body fails validation
- **THEN** the response is 400 `application/problem+json` listing each invalid field and its reason

#### Scenario: Collections paginate identically

- **WHEN** any collection endpoint is called with a page size and cursor
- **THEN** the response carries `items`, `total` and the next `cursor`, in the same shape as every
  other collection endpoint

### Requirement: The API is defined by a contract, not by its implementation

The system SHALL define its HTTP API in an OpenAPI document held in the repository, SHALL generate
the server interfaces and request and response types from that document, and SHALL implement each
endpoint against a generated interface, so that an implementation that no longer matches the
contract fails the build.

#### Scenario: Implementation drifts from the contract

- **WHEN** an endpoint's implementation no longer matches the operation the contract declares
- **THEN** the build fails, rather than the difference being found by a reader or a consumer

#### Scenario: The published document is the contract

- **WHEN** a consumer fetches the API document from the running service
- **THEN** it is the same contract the server interfaces were generated from, not a description
  rebuilt from the implementation

### Requirement: Themed application shell

The system SHALL present a single-page application shell with primary navigation Home, Specs,
Reviews, Projects and Activity, rendered with the SpecForge design tokens, defaulting to the dark
theme and offering a light theme whose choice persists for that browser.

#### Scenario: Active section is marked

- **WHEN** a user navigates to Reviews
- **THEN** the Reviews navigation item is marked active and the browser URL identifies the section

#### Scenario: Theme choice persists

- **WHEN** a user switches to the light theme and reloads
- **THEN** the light theme is still applied

#### Scenario: Empty states point at the next step

- **WHEN** no repository has been connected yet
- **THEN** each screen shows an empty state that names connecting a repository as the next step
