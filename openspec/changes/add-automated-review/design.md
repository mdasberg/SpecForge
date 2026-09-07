## Context

Agents produce a lot of plausible text quickly. In a review tool, that is a hazard as much as a
help: a confident wrong finding can steer a reviewer, and an approval that came from a machine is
worthless as an audit record. The design has to make agent participation useful without letting it
borrow human authority.

## Goals / Non-Goals

**Goals:**
- An agent's contribution is always identifiable as an agent's, at the API and in the UI.
- A human can dispose of every agent finding cheaply — accept, dismiss, or argue with it.
- Checks that gate approval are a deliberate, auditable choice, not a default of whoever wrote the check.

**Non-Goals:**
- Agents writing to the repository or editing specifications.
- Agents approving, requesting changes, or resolving human threads.
- A plugin marketplace for checks.

## Decisions

- **Agent identities live in the human identity table with `actor_kind = AGENT`.** Provenance is then
  structurally impossible to omit: every comment and finding already carries an author, and the author
  carries the kind. A separate agent table would allow a code path that forgets.
- **A check is a definition plus runs; findings belong to a run.** Reruns are then comparable ("this
  finding is new since the last push"), and a stale result stays inspectable instead of being deleted.
- **Blocking is a property of the check definition, set per project, not chosen by the agent.** The
  team decides what stops an approval. An agent cannot escalate its own severity into a block.
- **Accept creates a thread; it does not edit the spec.** Accepting a suggestion records intent and
  opens an anchored thread carrying the proposed text, which a human then takes to the repository.
  SpecForge stays read-only on the repository, so "apply" would be a lie.
- **Dismiss is cheap, recorded, and reversible while the review is open.** Requiring a justification
  would train reviewers to type "n/a"; recording who dismissed what, and letting it be undone, gets
  the accountability without the ceremony.
- **Model-backed checks and deterministic checks share one port and one result shape.** The review
  screen should not care that OpenSpec validation is a parser and security review is a model, and the
  set should be able to shift between the two without a UI change.
- **Findings carry the run reference, including model identity.** "Which model said this, on which
  content" is the first question asked when a finding is wrong.

## Risks / Trade-offs

- Model-backed checks are non-deterministic, so a rerun on identical content can differ. Accepted and
  made visible: results carry their run, and reruns are explicit rather than silent.
- Eight checks on every push is a cost. Deterministic checks run always; model-backed ones run on the
  changed sections only, and reruns are debounced per head version.
- Accept-creates-a-thread will read as a missing feature to anyone expecting a one-click fix. It is
  the honest consequence of the read-only rule, and it is stated in the UI copy.
