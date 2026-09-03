## 1. Catalog read model

- [ ] 1.1 Query for specifications with grouping by project, repository, domain or team, returning
      groups with counts and rows carrying title, path, status, owner, version and last change.
- [ ] 1.2 Combinable filters on status, owner, team, domain and tags over that query.
- [ ] 1.3 Paginate through the shared envelope, with a stable sort (last changed, then title).

## 2. Search

- [ ] 2.1 Denormalised `spec_search` table (document, version, section key, heading, text) rebuilt
      on version import, behind a `SpecSearch` port.
- [ ] 2.2 MySQL full-text ranked query returning the best matching section per document.
- [ ] 2.3 Search combines with the active filters rather than replacing them.

## 3. Document rendering

- [ ] 3.1 API returning a version's body, section tree and metadata in one response.
- [ ] 3.2 Frontend markdown renderer (remark) matching the prototype: headings, lists, tables,
      inline code, fenced code with language-aware highlighting, blockquotes.
- [ ] 3.3 Section outline with anchor links; opening a section anchor scrolls to and highlights it.
- [ ] 3.4 Version selector rendering any historical version, with the current version marked.

## 4. Screens

- [ ] 4.1 Specs screen: grouping control, filter chips, search box, result rows, empty and no-match states.
- [ ] 4.2 Projects screen: per project its connections, spec count by status and open reviews.
- [ ] 4.3 Encode grouping, filters and search in the URL query; restore state on load.

## 5. Verification

- [ ] 5.1 Tests: filters combine, grouping counts match row counts, search ranks a heading match above
      a body match, and a filtered URL restores the same view.
