## ADDED Requirements

### Requirement: Browse specifications with hierarchical grouping
The system SHALL list every specification the user may see and SHALL group the list by project,
repository, domain or owning team at the user's choice, showing the number of specifications in each
group and, per row, the title, path, lifecycle status, owner, current version and when it last changed.

#### Scenario: Group by project
- **WHEN** a user groups the specification list by project
- **THEN** each project appears once with its specification count, and its specifications are listed
  beneath it

#### Scenario: Regrouping preserves the result set
- **WHEN** a user switches grouping from project to domain
- **THEN** the same specifications are shown, regrouped, with counts that still sum to the total

### Requirement: Combinable filters
The system SHALL filter the specification list by lifecycle status, owner, owning team, domain and
tags, SHALL apply multiple filters together as a conjunction, and SHALL state when a combination
matches nothing.

#### Scenario: Filters combine
- **WHEN** a user filters on status In Review and domain Claims
- **THEN** only specifications that are both In Review and in the Claims domain are listed

#### Scenario: No match is explicit
- **WHEN** a filter combination matches no specification
- **THEN** the list shows an explicit no-match state naming the active filters, not an empty page

### Requirement: Search across specifications
The system SHALL search specification titles, repository paths, section headings and body text,
rank the results, and show for each result the section that matched, and SHALL apply search together
with any active filters.

#### Scenario: Body match shows its section
- **WHEN** a user searches for a term that appears in the Validation Rules section of one specification
- **THEN** that specification is listed with Validation Rules identified as the matching section

#### Scenario: Search respects filters
- **WHEN** a user searches while a status filter of Approved is active
- **THEN** only approved specifications appear in the results

### Requirement: Shareable views
The system SHALL encode the active grouping, filters and search term in the URL, and SHALL restore
exactly that view when the URL is opened again.

#### Scenario: A filtered view is shareable
- **WHEN** a user copies the URL of a list grouped by team and filtered to Changes Requested, and
  another user opens it
- **THEN** the second user sees the same grouping and filter, applied to the specifications they may see

### Requirement: Rendered specification document
The system SHALL render a specification version as a technical document — headings, lists, tables,
inline code and fenced code blocks with language-aware highlighting — alongside its metadata and a
section outline, and SHALL provide a deep link per section.

#### Scenario: Code blocks render as code
- **WHEN** a specification containing a fenced JSON contract and a fenced Java snippet is opened
- **THEN** both render as code blocks with their language's highlighting, not as body text

#### Scenario: Section deep link
- **WHEN** a user opens a link addressing the API Contract section of a specification
- **THEN** the document opens scrolled to that section with it highlighted

### Requirement: Read any historical version
The system SHALL let a reader select any version of a specification and render it, marking which
version is current and when each version was created and by whom.

#### Scenario: Reading an older version
- **WHEN** a reader selects version 1 of a specification currently at version 3
- **THEN** version 1's content is rendered, labelled as an older version, with its author and date

### Requirement: Project overview
The system SHALL show, per project, its connected repositories, its specification counts by
lifecycle status, and its open reviews.

#### Scenario: Project shows its state
- **WHEN** a user opens the Care Management project
- **THEN** its connected repositories, specification counts per status and open reviews are listed
