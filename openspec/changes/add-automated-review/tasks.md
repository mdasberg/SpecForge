## 1. Agent identities

- [ ] 1.1 Seed agent identities in the identity table with `actor_kind = AGENT`, a display name and an avatar.
- [ ] 1.2 A Keycloak service-account client per check runner (client credentials grant), resolved to its
      agent identity on token validation by the mechanism `add-project-skeleton` established; add the
      clients to `keycloak/realm-export.json` for development.
- [ ] 1.3 Guard: a request authenticated as an agent cannot reach verdict or thread-resolution endpoints.

## 2. Check definitions and runs

- [ ] 2.1 `check_definition` per project: key, display name, blocking flag, runner kind, enabled flag.
- [ ] 2.2 `check_run` per review head: state (queued, running, passed, failed, skipped), started and
      finished timestamps, duration, summary, stale flag.
- [ ] 2.3 `CheckRunner` port with one deterministic and one model-backed implementation; dispatch on
      review head created or advanced.
- [ ] 2.4 Rerun a single check or all checks on the current head; mark superseded runs stale rather than
      deleting them.
- [ ] 2.5 Debounce: a further head advance cancels queued runs for the superseded head.

## 3. The initial check set

- [ ] 3.1 OpenSpec structural validation (requirements have scenarios, headings well-formed) — deterministic, blocking.
- [ ] 3.2 Acceptance-criteria coverage: every requirement has at least one scenario — deterministic, blocking.
- [ ] 3.3 API compatibility: contract blocks diffed against the base version for breaking shape changes — deterministic, blocking.
- [ ] 3.4 Architecture rules: project-configured rule list checked against the spec — deterministic, advisory.
- [ ] 3.5 Terminology consistency against a project glossary — deterministic, advisory.
- [ ] 3.6 Security review, missing edge cases and breaking-change analysis — model-backed, advisory,
      scoped to changed sections.

## 4. Findings and dispositions

- [ ] 4.1 `finding`: check run, anchor (section key plus optional text range), severity, title, body,
      optional proposed text, author agent identity.
- [ ] 4.2 Findings render through the same anchored presentation as human threads, with the agent treatment.
- [ ] 4.3 Accept: record the disposition and open an anchored thread carrying the proposed text, attributed
      to the accepting human with the agent's suggestion quoted.
- [ ] 4.4 Dismiss: record actor and timestamp, hide from the active list, keep in history.
- [ ] 4.5 Discuss: open a thread on the finding's anchor without a disposition.
- [ ] 4.6 Undo an accept or dismiss while the review is open.

## 5. Gate integration and screens

- [ ] 5.1 Expose the blocking check state to the approval gate's read port; advisory results never block.
- [ ] 5.2 Checks tab: check list with state, duration, blocking marker, findings per check, rerun action.
- [ ] 5.3 Review panel check summary line, matching the counts on the Checks tab.

## 6. Verification

- [ ] 6.1 Tests: an agent identity cannot approve, request changes or resolve a human thread.
- [ ] 6.2 Tests: a failed blocking check blocks approval; a failed advisory check does not.
- [ ] 6.3 Test: head advance marks prior runs stale, dispatches new runs, and preserves dispositions
      on findings whose anchors survived.
