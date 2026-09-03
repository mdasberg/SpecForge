# spec-document

## Purpose

The specification itself, as SpecForge holds it: an identity tied to the repository path it
mirrors, metadata derived on import, an immutable content-addressed version per state the file has
been in, a section tree whose keys are what discussions and diffs address, and one lifecycle status
governed by a single transition table.

## Requirements

### Requirement: Specification document identity and metadata
The system SHALL represent each specification as a document identified by its connection and
repository path, carrying a title, project, domain, owning team, owner, tags and lifecycle status,
where the title is derived from the document's first heading.

#### Scenario: Metadata derived on import
- **WHEN** a file at `openspec/specs/claim-preauthorization/spec.md` is imported on a connection
  belonging to project Care Management
- **THEN** the document's project is Care Management, its domain is derived from the path segment,
  its owner is the last commit author, and its title is the file's first heading

#### Scenario: Path is the stable identity
- **WHEN** the same path is imported again after unrelated repository changes
- **THEN** the existing document is updated rather than a second document being created

### Requirement: Immutable content-addressed versions
The system SHALL store each imported state of a document as an immutable version with a monotonic
ordinal, the content, a content hash, the commit reference, the authoring identity and a timestamp;
SHALL create no new version when the content hash is unchanged; and SHALL never modify a version
once created.

#### Scenario: Unchanged content creates no version
- **WHEN** a document at version 3 is re-imported from a later commit with identical content
- **THEN** no version is created and version 3 remains current

#### Scenario: Changed content creates the next version
- **WHEN** a document at version 3 is imported with changed content
- **THEN** version 4 is created carrying the new content hash, commit reference, author and timestamp,
  and version 3 stays byte-identical

#### Scenario: Versions are immutable
- **WHEN** any request attempts to alter a stored version's content
- **THEN** the request is refused and the stored content is unchanged

### Requirement: Section structure with stable anchors
The system SHALL parse each version into a tree of sections keyed by heading slug and ordinal, and
SHALL expose those keys as the anchor targets that discussions and diffs address.

#### Scenario: Sections are addressable
- **WHEN** a version containing the headings Purpose, Preconditions and API Contract is imported
- **THEN** each becomes a section with a stable key, in document order, with its nesting preserved

#### Scenario: An unrelated edit keeps other anchors
- **WHEN** a new version changes only the body of the API Contract section
- **THEN** every other section keeps the key it had in the previous version

#### Scenario: A renamed heading orphans its anchor
- **WHEN** a new version renames the heading Preconditions
- **THEN** the old section key is absent from the new version, and anchors addressing it are
  reported as outdated rather than reattached to another section

### Requirement: Lifecycle status
The system SHALL hold one lifecycle status per document — Draft, In Review, Changes Requested,
Approved or Implemented — and SHALL permit only the transitions Draft or Changes Requested to
In Review, In Review to Changes Requested or Approved, Approved to Implemented, and Approved or
Implemented to In Review when a new change is proposed.

#### Scenario: Proposed change moves a draft into review
- **WHEN** a change is proposed for a document at status Draft
- **THEN** the document's status becomes In Review

#### Scenario: Illegal transition is refused
- **WHEN** a transition from Draft directly to Approved is attempted
- **THEN** the transition is refused and the status is unchanged

#### Scenario: An approved spec re-enters review
- **WHEN** a new change is proposed for a document at status Approved
- **THEN** the document's status becomes In Review and the earlier approval stays recorded in history
