---
name: cr
description: >-
  Review Unity Java changes in a branch or explicit commit set against a task description, using specialist
  sub-agents for tests, correctness and completeness, code quality, and security. Target defaults to dev and
  supports version branches such as dev-x.y. Use when a Unity code review is requested.
---

# Unity Code Review

Invoke as `$cr` or through the `$unity-cr` alias.

Produce an evidence-based review of the requested changes against the realized task description. Review the
implementation and its tests; do not implement fixes, commit changes, or post the report to an external service
unless the user requests those actions.

## Collect and normalize inputs

Require these inputs, taking values already supplied in the conversation:

- **Scope:** either a source branch or an explicit set of commits. Never assume the current checkout is the
  requested source. If both are supplied and their relationship is unclear, clarify the selection.
- **Target:** use `dev` unless the user supplies another target, such as `dev-4.2`. Do not substitute `main`,
  the remote default branch, or a different version branch when the target is missing.
- **Task description:** the expected behavior, constraints, and acceptance criteria supplied as text, inline or
  in a user-designated local text file. A ticket ID or commit message alone does not replace the description.

If scope or task text is missing, request the missing information together. Continue independent repository and
scope discovery while waiting, but do not finish a completeness assessment without the task description.
Ask only about ambiguities that change the review; do not require a ticket ID or a template.

Preserve the supplied task text and its source. Extract numbered acceptance criteria without inventing new
requirements. Separate explicit requirements from assumptions and unresolved questions. Record all three in the
review context. The current intake accepts text; ticket retrieval is a future adapter that must produce the same
task-text/source fields. Do not invent a ticket integration or fetch a ticket in this version.

Examples of sufficient requests:

```text
Use $cr for branch feature/invitations.
Task: Managers can revoke pending invitations. Revoked invitations cannot be accepted.

Use $cr for commits abc1234 def5678 against dev-4.2.
Task: Invalid date expressions must return a validation error and preserve the saved configuration.
```

## Freeze a shared review context

Read the applicable `AGENTS.md` instructions, inspect the working-tree status, and read
[references/scope-and-validation.md](references/scope-and-validation.md) before resolving scope or running checks.
Use [scripts/resolve_scope.py](scripts/resolve_scope.py) to resolve immutable SHAs and comparison pairs. It is
read-only; it does not fetch, check out branches, or change files. Review exactly the scope selected by the user.

Create a temporary review context outside the working tree containing:

- The scope manifest, resolved target/source SHAs, canonical patches, and changed paths.
- Original task text and provenance, extracted acceptance criteria, assumptions, and open questions.
- Applicable repository instructions and a short map of affected modules, entry points, and relevant tests.
- Shared validation evidence: commands, snapshot tested, actual test counts/results, and limitations.

All reviewers receive this same context and paths to the relevant skill references. Use committed snapshots or
an isolated worktree so unrelated local changes do not become evidence. Inspect callers and surrounding code
when necessary to understand a changed path. Existing defects outside the selected changes are not new findings
unless the change exposes them or they prevent an explicit requirement from being met.

## Delegate the review

Use several independent sub-agents. The coordinator owns scope, shared test execution, and synthesis. Read
[references/reviewers.md](references/reviewers.md) and give each reviewer its corresponding brief:

| Reviewer | Responsibility |
| --- | --- |
| Testing | Unit/integration coverage and issues, test pyramid, behavioral assertions, and pre-existing test duplication |
| Correctness and completeness | Bugs, regressions, acceptance criteria, compatibility, and migrations |
| Code quality | Abstraction, cohesion, naming, comments, feature packages, encapsulation, and immutable types |
| Security | Changed trust boundaries, authorization, data isolation, unsafe inputs/outputs, and sensitive data |

Run reviewers in waves within the available concurrency. With three child-agent slots, launch the first three
reviewers, then reuse a completed slot for security. Preserve
independence: send each reviewer the shared artifacts and its assignment, rather than another reviewer's proposed
findings. Reviewers may read adjacent code, request focused checks from the coordinator, and identify overlap.
They must not modify code, run competing Maven builds, or delegate further without the coordinator's allocation.

Use this delegation shape, adapting paths and the assigned lens:

```text
Review the frozen scope in <context-path> using the <lens> brief in <skill-path>/references/reviewers.md.
Compare the changes with the supplied task and applicable AGENTS.md. Inspect actual implementation and tests.
Return actionable findings with priority, exact path/line and snapshot, trigger, consequence, evidence, and
recommended correction. Also return inspected areas, task/test mappings relevant to your lens, checks needed,
and limitations. Acknowledge criteria that are not applicable or unverified. Do not edit or build the repository.
```

If delegation tools are unavailable, disclose the limitation and perform the same lenses sequentially. Do not
claim that independent reviewers participated.

## Verify and reconcile

Run focused checks appropriate to changed behavior using the shared validation guidance. Existing meaningful
tests can provide coverage; no arbitrary coverage percentage, fixed layer ratio, or new-file quota is required.
If tests cannot run, continue static review and identify what remains unverified. Never equate source inspection,
a skipped suite, a successful build without tests, or JaCoCo line coverage with passing behavioral tests.

Validate every candidate finding against the canonical patch and relevant final code. Check the trigger and
consequence; use a minimal reproduction or a focused test where it materially resolves uncertainty. Check whether
a selected commit's apparent defect is corrected by another selected commit. For noncontiguous sets, distinguish
selected-change defects from prerequisite limitations or fixes supplied only by excluded commits.

Merge duplicate findings across lenses and retain all relevant evidence. Resolve conflicting conclusions by
tracing the code or asking the reviewers for a specific follow-up; do not decide by vote. Report uncertain claims
as questions or verification gaps. Keep actionable maintenance recommendations distinct from correctness and
security defects. Do not pad the report with speculative risks or inherited style violations.

Use these priorities consistently:

- **P0:** demonstrated critical impact requiring immediate action, such as a broadly exploitable bypass or data loss.
- **P1:** serious defect or missing core requirement that should block merging.
- **P2:** actionable defect, coverage gap, or design problem with a bounded impact.
- **P3:** concrete low-risk maintainability improvement; keep these brief.

## Deliver the review

Lead with the actionable findings, ordered by priority. For each give a short title, path and precise line at the
reviewed snapshot, triggering condition, impact, supporting evidence, and recommended correction. Cite the
relevant task criterion where useful. For omissions, anchor the finding to the nearest relevant implementation
and name the missing artifact or behavior. Use local clickable links only into a persistent checkout matching the
reviewed snapshot. For disposable worktrees or a different current checkout, identify the commit SHA and repository
path so removing review artifacts does not break the final report's references.

Include a concise scope/task summary with the resolved target and source or selected SHAs. Provide a compact
acceptance-criterion mapping with **implemented**, **partial**, **missing**, or **unverified** status and evidence.
Distinguish implementation evidence from test evidence, including the unit/integration coverage gaps and any
browser-test justification. Summarize actual checks and outcomes, unrun checks and reasons, and remaining questions.

Always include a comprehensive test-review table using the format in
[references/reviewers.md](references/reviewers.md#required-testing-table). Group rows into unit-test gaps/issues,
integration-test gaps/issues, and, when found, duplication with pre-existing tests. Enumerate every supported issue
within the reviewed scope; a one-line summary or selection of examples is insufficient. Preserve the testing
reviewer's detail during synthesis, including issues that do not appear among the highest-priority findings.
When a gap relates to a defect already reported, reference that finding while retaining the concrete missing
scenario and required assertions in the table. Consolidate repeated reports of the same issue without dropping
distinct gaps. State explicitly when a layer has no identified issues, is not applicable, or remains unverified.

If no actionable findings are supported, state that plainly and still disclose the validation limits. A clean
review does not prove complete coverage or absence of security defects. Do not claim the task is complete while
required behavior or relevant validation remains unverified. Remove owned temporary artifacts once no longer
needed; preserve useful logs and identify their location when investigation is still pending.
