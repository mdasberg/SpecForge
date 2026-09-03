## ADDED Requirements

### Requirement: Specification to deployment chain
The system SHALL present, for an approved specification version, the chain from that version through
its approval, its implementation tickets, the pull requests implementing it and the deployments of
those pull requests, with each link carrying its source and timestamp.

#### Scenario: Complete chain
- **WHEN** an approved specification has a linked ticket whose branch was merged in a pull request that
  deployed to production
- **THEN** the chain shows the version, the approval with its approvers, the ticket, the pull request and
  the deployment, each with when it happened

#### Scenario: Chain is per version
- **WHEN** a specification has approvals on version 2 and version 4
- **THEN** each approved version has its own chain, and work traced to version 2 is not shown as
  implementing version 4

### Requirement: Pull request and deployment discovery
The system SHALL associate pull requests with a specification version through the review's originating
pull request and through branches referenced by linked tickets, and SHALL record deployments from
inbound deployment events, accepting both the source forge's deployment events and a generic ingest
endpoint.

#### Scenario: Review's pull request is in the chain
- **WHEN** a review originated from a pull request
- **THEN** that pull request appears in the chain for the version it reviewed

#### Scenario: Deployment recorded from a pipeline
- **WHEN** a deployment event for a merged pull request arrives, naming its environment
- **THEN** the deployment is recorded against that pull request with its environment and timestamp

#### Scenario: Specification becomes Implemented
- **WHEN** a pull request implementing an approved specification deploys to production
- **THEN** the specification's status becomes Implemented and the transition is recorded

### Requirement: Gaps are explicit
The system SHALL identify and display gaps in the chain — an approved version with no implementation
ticket, a ticket with no pull request, a merged pull request with no deployment — rather than
presenting an incomplete chain as complete.

#### Scenario: Approved but not ticketed
- **WHEN** a specification has been approved and no implementation ticket is linked
- **THEN** the chain shows a gap stating that no implementation ticket exists

#### Scenario: Merged but not deployed
- **WHEN** an implementing pull request is merged and no deployment has been recorded
- **THEN** the chain shows a gap stating that no deployment has been recorded, and the specification is
  not marked Implemented
