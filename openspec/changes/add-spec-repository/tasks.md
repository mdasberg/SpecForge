## 1. Forge access

- [ ] 1.1 GitHub App registration and installation callback; persist installation, granted repositories, account.
- [ ] 1.2 Installation-token client (contents, tree, pull requests, commit statuses) behind a `Forge` port.
- [ ] 1.3 Inbound webhook endpoint with HMAC signature verification, delivery-id replay guard and event dispatch.
- [ ] 1.4 Handle revoked or suspended installations by marking the connection degraded, retaining imported data.

## 2. Scan (the wizard's second step)

- [ ] 2.1 Resolve the path glob against a branch's tree; return matched paths.
- [ ] 2.2 Classify each match: importable spec, change proposal, unparsable (with reason).
- [ ] 2.3 Asynchronous scan with progress and a cached result the wizard can re-read.
- [ ] 2.4 Refuse to connect when the glob matches nothing, or when the repository plus branch plus path
      is already connected.

## 3. Spec document model

- [ ] 3.1 Tables: `spec_document` (project, domain, team, owner, tags, path, connection, status),
      `spec_version` (ordinal, content sha, body, author, commit sha, created at), `spec_section`.
- [ ] 3.2 Markdown parser producing a section tree with heading-slug-plus-ordinal keys.
- [ ] 3.3 Content normalisation plus sha256 addressing; `importVersion` returns the existing version
      unchanged when the sha matches.
- [ ] 3.4 Lifecycle state machine with the five statuses and the legal transitions, as the single
      writer of `spec_document.status`.

## 4. Import and sync

- [ ] 4.1 Initial import: create documents and version 1 for every importable match; record an import run.
- [ ] 4.2 Push sync: on a push to the connected branch, import changed matched files as new versions.
- [ ] 4.3 Pull-request sync (default): on PR open or synchronise, record the PR head as a candidate
      version set and emit `SpecChangeProposed` for `add-spec-review` to consume.
- [ ] 4.4 Manual re-import endpoint, idempotent against unchanged content.
- [ ] 4.5 Outbound commit status on the PR: pending on proposal, and a hook the approval capability
      later drives to success or failure.

## 5. Project configuration

- [ ] 5.1 Persist project name, team, domains, tracker and approval rule captured in the wizard's third
      step (the rule is stored here and evaluated by `add-review-approval`).
- [ ] 5.2 Derive spec metadata on import: project from the connection, domain from the path segment,
      owner from the last commit author, tags from front matter when present.

## 6. Read-only guarantee and verification

- [ ] 6.1 Reject any attempt to mutate spec content through the API with 409 problem+json naming the repository.
- [ ] 6.2 Architecture test asserting no module outside `repository` reaches the `Forge` port, and that
      the port exposes no content-write operation.
- [ ] 6.3 Integration tests over a fake forge: initial import, unchanged re-import, changed re-import,
      PR sync, revoked installation.
