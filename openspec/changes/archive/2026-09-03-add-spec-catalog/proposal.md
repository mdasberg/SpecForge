## Why

Imported specifications are invisible until they can be found and read. The catalog is the
everyday surface: hundreds of specs across projects, repositories, domains and teams, grouped so a
reader can start from where they work, filtered so a reviewer can see only what concerns them, and
rendered well enough that a technical document with code blocks, tables and API contracts is
actually readable. Reading a spec is also the entry point to reviewing one, so the rendered
document and its section anchors are built here and reused by the review screens.

## What Changes

- Spec browser: list every specification with hierarchical grouping by project, repository, domain
  or team, with counts per group.
- Combinable filters on status, owner, team, domain and tags, plus free-text search over titles,
  paths, headings and body, with the matching section shown on each hit.
- Filter and grouping state encoded in the URL so a view can be shared or bookmarked.
- Rendered specification document: markdown with code blocks, tables and inline code, a section
  outline, deep links per section, and a version selector that renders any historical version.
- Project overview listing each project's specs, connections and open reviews.

## Capabilities

### New Capabilities
- `spec-catalog`: browsing, grouping, filtering, searching and rendering specification documents
  and their versions.

### Modified Capabilities
(none — `spec-document` gains no new requirements; the catalog reads it)

## Impact

- New module `catalog` read models plus a search index. Full-text search starts as a PostgreSQL
  full-text index over a denormalised `spec_search` table rebuilt on version import; swapping in a
  dedicated search engine stays behind the search port.
- Frontend: Specs and Projects screens, and the specification document view reused by the review
  screens in `add-spec-review`.
- Markdown rendering is a frontend concern (remark), fed by the section tree the backend already
  parses; the backend does not emit HTML.
- Depends on: `add-spec-repository`.
