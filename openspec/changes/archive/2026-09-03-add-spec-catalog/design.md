## Context

Imported specifications are invisible until they can be found and read. The catalogue is the
everyday surface — hundreds of documents across projects, repositories, domains and teams — and it
is also the entry point to reviewing one, so the rendered document and its anchors built here are
what the review screens reuse.

Two constraints shaped every decision below. The first is the module graph: `repository` already
depends on `catalog` (its importer calls `SpecCatalog`), so anything the catalogue needs from the
repository module would be a cycle. The second is that the connected repository is read-only, which
makes every table here a derived read model that an import can rebuild from scratch.

## Goals / Non-Goals

**Goals:**
- Find a specification by where it lives, by who owns it, or by what it says.
- Group counts and filters that still agree with each other after a filter is added to one of them.
- A rendered document whose section anchors are the same ones a discussion will address.

**Non-Goals:**
- Editing a specification (the repository is read-only).
- Reviews, threads or approvals (steps 4-6).
- A facets endpoint, or search across anything but the current version.

## Decisions

- **The catalogue answers from its own tables, including for repositories and projects.**
  `repository_full_name` is denormalised onto `spec_document` and refreshed on every import, and
  `/api/projects` is built by grouping `spec_document` rather than by reading
  `repository_connection` and `project`. This is not a performance choice: `repository` already
  depends on `catalog`, so a read back the other way is a cycle and `ModularityTests` fails. The
  cost is a name that goes stale until the next import, which is the same staleness every other
  imported field already has.

- **Full-text search is PostgreSQL's, over a denormalised `spec_search` table.** The proposal was
  written against MySQL; the stack moved to PostgreSQL, so the index is a `tsvector` in a **stored
  generated column** — the database computes it from `heading` and `body`, which means the index
  cannot disagree with the row it indexes, and there is no second writable path to it. The heading
  is `setweight`ed A and the body B, so a heading match outranks a body match without the query
  having to say so.

- **A section is indexed with its own text only**, up to the next heading at any level, never with
  the text of the sections it encloses. Indexing enclosed text too makes the outermost section the
  best match for every term in the document, which defeats the point of naming *where* a term is.
  A synthetic row with an empty anchor key carries the title and path, so those are searchable
  through the same index rather than through a second `OR` in every query.

- **Only the current version is indexed**, and the rows are deleted and rebuilt on each version
  import. A search hit on text that has since been rewritten points at nothing.

- **One assembled predicate serves the rows, the total and the group counts.** The three queries
  are built from one `FROM`/`WHERE` fragment because they must agree: counts that sum to a total
  the rows contradict is the bug this shape makes impossible. Nothing from the request is
  concatenated into SQL — the only text chosen by code is a column name from a fixed table keyed by
  an enum, and every value is bound.

- **The list envelope is a generated contract type, not `Page<T>`.** It carries the same items,
  total and cursor, plus the group counts. The API's types come from the contract and a generic
  cannot be expressed there, so `SpecList` mirrors `Page` rather than reusing it.

- **The cursor is an offset, and it is validated.** It arrives in a URL and a URL is edited, so a
  cursor that is not a non-negative integer is a bad request rather than an argument to a query.
  A page is fetched one row longer than asked for, so "is there a next page" is a fact about the
  result instead of a second count that can disagree with it.

- **The document is returned with its version and its whole version list in one response.** Opening
  a specification is one request, and the version selector needs the full list to mark which entry
  is current.

- **Search snippets are marked with `[[` and `]]`, deliberately not with HTML.** The snippet is
  body text from someone else's repository crossing to a browser; the client splits on the markers
  and renders `<mark>` elements itself, so no path exists where repository content is rendered as
  markup.

- **Markdown rendering is the client's job.** The backend serves the normalised source and the
  section outline it already parsed, and never emits HTML. Section ids in the rendered document
  come from the backend's `anchorKey` rather than from a slug rule reimplemented in TypeScript —
  a second implementation of the anchor rule is free to disagree with the one discussions and diffs
  address.

- **The outline is a flat list with a level, not a tree.** The nesting is rebuilt from `level` by
  whoever renders it, which keeps a self-referencing schema out of the contract.

## Risks / Trade-offs

- `openReviews` is the count of specifications in `IN_REVIEW`. The review capability does not exist
  until step 4; this is the honest answer until it does, rather than a zero standing in for one,
  and it becomes a real count of open reviews there.
- A project appears only once it has an imported specification, so a connection whose import has
  not finished is briefly absent from `/api/projects`. Accepted: a connection that matches nothing
  importable is refused at creation, so this is a window, not a state.
- Offset paging can skip or repeat a row if a document is imported between two pages. Accepted at
  this scale; a keyset cursor on `(updated_at, title)` is the upgrade, and it does not apply to the
  search ordering, which is by rank.
- Filter chip values are derived from the loaded page rather than from a facets endpoint, so a
  value that exists only outside the current result set cannot be discovered by clicking. Marked in
  the code; a facets endpoint is the upgrade.
- The English text search configuration is hard-coded. A specification written in another language
  stems wrongly. Accepted for now — every specification SpecForge is built for is written in
  English, and the configuration is one string in one query.
