## ADDED Requirements

### Requirement: Connect a repository that holds specifications
The system SHALL let an administrator connect a repository from a granted GitHub App installation
by choosing the repository, a branch, a path glob defaulting to `openspec/specs/**/spec.md`, and a
specification format, and SHALL refuse a connection that duplicates an existing repository,
branch and path combination.

#### Scenario: Connect a granted repository
- **WHEN** an administrator selects a granted repository, confirms the branch and the default path
  glob, and completes the wizard
- **THEN** the connection is persisted and an initial import is started

#### Scenario: Already-connected repository is refused
- **WHEN** an administrator tries to connect a repository, branch and path that are already connected
- **THEN** the connection is refused with a message naming the existing connection

#### Scenario: Ungranted repository is not offered
- **WHEN** the installation grants four of twenty-three repositories
- **THEN** only those four are selectable, and the wizard states how many of how many are granted

### Requirement: Scan before connecting
The system SHALL, before a connection is created, scan the chosen branch and glob and report every
matched file classified as an importable specification, a change proposal, or unparsable with a
reason, together with the counts per class, and SHALL refuse to connect when nothing is importable.

#### Scenario: Scan classifies matched files
- **WHEN** the glob matches twelve specification files, three change proposals and one malformed file
- **THEN** the scan reports twelve importable, three change proposals and one unparsable with the
  parse reason for that file

#### Scenario: Glob matching nothing blocks the wizard
- **WHEN** the glob matches no file on the chosen branch
- **THEN** the wizard cannot proceed and states that no specification was found at that path

### Requirement: Import creates documents and initial versions
The system SHALL, on initial import, create one specification document per importable file with its
project, domain, team, owner, tags and repository path, each holding an initial version at status
Draft, and SHALL record the import run with its per-file outcomes.

#### Scenario: Initial import
- **WHEN** the initial import of a connection with twelve importable files completes
- **THEN** twelve specification documents exist, each with version 1 at status Draft, and the import
  run records twelve successes

#### Scenario: Import outcome is inspectable afterwards
- **WHEN** a user asks why a file at a matched path has no specification document
- **THEN** the import run for that connection shows that file's outcome and reason without re-running

### Requirement: Synchronisation modes
The system SHALL support three synchronisation modes per connection — on pull request (the
default), on push, and manual — where pull-request synchronisation records the pull request's head
content as a proposed change and emits a change-proposed event, push synchronisation imports
changed matched files as new versions, and manual synchronisation re-imports on request.

#### Scenario: Pull request proposes a change
- **WHEN** a pull request is opened that modifies a matched specification file on a connection in
  pull-request mode
- **THEN** the pull request's head content is recorded as a proposed change for that specification
  and a change-proposed event is emitted carrying the pull request reference

#### Scenario: Pull request update replaces the proposal head
- **WHEN** a further commit is pushed to that pull request
- **THEN** the proposed change's head content is updated rather than a second proposal being created

#### Scenario: Push mode versions the default branch
- **WHEN** a commit lands on the connected branch of a connection in push mode, changing two matched files
- **THEN** two new specification versions are created and no proposed change is recorded

#### Scenario: Manual synchronisation is idempotent
- **WHEN** manual synchronisation runs twice with no repository change in between
- **THEN** the second run creates no version and reports no change

### Requirement: Report review state back to the pull request
The system SHALL report the state of a specification review back to the originating pull request as
a commit status, pending while the review is open, successful when the specification is approved,
and failed when changes are requested.

#### Scenario: Status is pending on proposal
- **WHEN** a pull request proposes a specification change
- **THEN** a pending commit status is posted to the pull request head naming the SpecForge review

#### Scenario: Status follows the verdict
- **WHEN** the review for that pull request reaches Approved
- **THEN** the commit status is updated to successful

### Requirement: The repository stays read-only
The system SHALL never write specification content to a connected repository, and SHALL refuse any
request that would edit specification content in SpecForge, directing the user to the repository
instead. The only outbound write permitted is the review status on a pull request.

#### Scenario: Editing a spec in SpecForge is refused
- **WHEN** a user attempts to change the body of a specification version through the API
- **THEN** the request is refused with a conflict problem document naming the source repository and path

#### Scenario: No content-write path exists
- **WHEN** the forge port is inspected by the architecture test
- **THEN** it exposes no operation that creates a commit, branch, pull request or comment

### Requirement: Degraded connections retain history
The system SHALL mark a connection degraded when its installation is revoked, suspended or loses
access to the repository, keep every imported document and version readable, and stop
synchronising until access is restored.

#### Scenario: Revoked installation degrades the connection
- **WHEN** the GitHub App installation granting a connection is revoked
- **THEN** the connection is marked degraded, synchronisation stops, and previously imported
  specifications remain readable with a notice on the connection
