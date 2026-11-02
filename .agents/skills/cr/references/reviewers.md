# Specialist reviewer briefs

Each reviewer consumes the frozen scope and original task text. Inspect actual code and existing relevant tests,
including unchanged tests; diff-only reading cannot establish behavior, coverage, or compatibility. Return findings
only when supported by a specific changed path, unmet task requirement, or newly exposed issue. Mark irrelevant
checks as not applicable and unavailable evidence as unverified.

## Testing

Map each changed behavior and applicable acceptance criterion to tests, separating unit, integration, and browser
coverage. Include happy paths, meaningful boundaries, invalid inputs, error handling, regressions, and authorization
failures where relevant. Require comprehensive coverage for new services, parsers, and transformations. Identify
the uncovered scenario, plausible regression, and lowest suitable test layer for each coverage finding.

Use the requested test pyramid:

- **Unit tests provide the maximum behavior coverage.** Keep business rules, transformations, validation, and
  boundary combinations here, using controlled dependencies where appropriate.
- **Integration tests verify wiring and boundary contracts.** Check real Spring configuration, persistence mapping,
  serialization, transaction boundaries, or protocol adapters as applicable. Assertions may verify behavior across
  that boundary, but do not repeat the unit test matrix through an assembled system.
- **Browser E2E is rare.** Require a complex, UI-heavy, distinct workflow whose meaningful risk depends on frontend
  orchestration. Trace why lower-level tests cannot establish that behavior. A Vaadin change alone does not justify
  a browser suite; an important frontend-dependent workflow should not be left untested simply because E2E is rare.

Judge tests by the behavior they exercise and the dependencies they use, rather than directory names or raw counts.
An embedded database or Spring context test in `src/test/java` can still be an integration test. Do not impose fixed
ratios, require integration tests for isolated logic, or demand new tests where existing tests meaningfully cover
the change.

Check whether assertions observe returned values, persisted state, visible errors, emitted events, or other contract
effects. A test should detect a broken requirement while tolerating a harmless internal refactor. Flag tests that
only verify internal helper calls, reproduce the implementation algorithm as the expected result, or mock away the
logic under test. Mockito `verify` is appropriate when the interaction is itself an external contract, such as one
notification being delivered, or proving a forbidden write never happens. Do not treat all interaction assertions
as implementation restatement.

Check applicable repository test conventions: JUnit 5, Mockito, AssertJ, `*Test` discovery, `should...` method names,
given/when/then organization, parameterized scenarios using readable record-with-builder arguments, and no reflective
access to private implementation. Browser tests belong in `integration-tests/`. BDD structure does not require
redundant given/when/then comments. Assess fixtures, isolation, determinism, disabled tests, and whether the build
actually discovers the intended tests.

Treat JaCoCo data as supporting evidence at the matching snapshot. High line coverage cannot prove strong assertions
or complete behavior coverage; missing reports do not establish absent tests. Return the behavior-to-test map,
coverage gaps, misplaced test layers, weak assertions, and focused validation requests.

Compare new and modified tests with relevant pre-existing tests, including unchanged tests in other affected
modules and test layers. Inspect the tested scenario, setup, exercised path, and assertions rather than relying
on matching names or text. Identify the pre-existing counterpart in the applicable review baseline: merge base
for a branch, or the selected commit's parent for a commit-set review. Report duplication introduced or exposed
by the selected changes, without expanding the review into unrelated historical duplicates.

Call out duplication when a test repeats existing behavioral coverage without protecting a distinct contract,
boundary, or regression. Cite both tests and explain the overlap and any unique coverage that must be preserved.
An integration test exercising real wiring can legitimately overlap a unit test's business outcome; shared
fixtures, parameterized variants, or similar assertions alone do not prove duplication. Recommend reusing,
extending, consolidating, removing, or moving tests only after establishing which coverage would remain.

### Required testing table

Return a comprehensive table for the final review, with one row per distinct supported gap or issue:

| ID / priority | Category | Behavior / task criterion | Test evidence | Gap or issue and risk | Required correction |
| --- | --- | --- | --- | --- | --- |

Use these categories, grouped in this order:

1. **Unit tests:** all identified coverage gaps and test-quality issues, such as missing boundaries or denial
   scenarios, weak assertions, implementation restatement, isolation problems, or undiscovered tests.
2. **Integration tests:** all identified missing wiring/boundary checks and test-quality issues, including
   business-rule matrices that belong in unit tests. Do not demand integration tests for isolated logic.
3. **Duplication:** include rows when duplication with pre-existing tests is established. Identify the layers of
   both tests, including overlap between unit and integration tests where relevant.

For every actionable row, identify the precise scenario or defect, affected behavior or criterion, and supporting
test file/class/method and line at the frozen snapshot. If a test is missing, cite the relevant implementation
and the existing tests inspected. For duplication, cite both the reviewed test and its pre-existing counterpart,
with the baseline snapshot when needed. Explain the concrete regression risk, assertion weakness, unnecessary
maintenance cost, or misplaced test layer. Specify the missing inputs, expected observable outcome/assertions,
or exact consolidation needed; do not use generic instructions such as "add more tests."

Do not limit the table to examples, a fixed number of rows, or the most severe findings. Keep distinct scenarios
separate when they need different tests or corrections; combine only genuinely repeated reports. Reference any
corresponding main finding without replacing the table's scenario and correction with a bare cross-reference.
The table complements the behavior-to-test map and must survive the coordinator's report synthesis.

Always account for both unit and integration tests. If a category has no supported issue, include a status row
stating **no issues identified**, **not applicable**, or **unverified**, with inspected scope and the reason or
limitation. Do not treat absent evidence as a confirmed gap or an unassessed category as clean. If no duplication
was found, state that separately with the inspected scope; if the comparison was incomplete, disclose that limit
instead of claiming there are no duplicates. Include any justified browser-test issue in the table with an
explicit browser/E2E category; the detailed table does not replace the existing test-pyramid rules.

## Correctness and completeness

Trace the changed entry points through callers, state transitions, and failure paths. Evaluate null/empty inputs,
boundaries, ordering, concurrency, transactionality, cleanup, exception handling, time/date semantics, and API or
configuration compatibility where the changed logic makes them relevant. Identify a realistic trigger and resulting
incorrect behavior for each defect rather than listing generic risks.

Map each explicit acceptance criterion to implementation paths and test evidence. Check required behavior is wired
into reachable entry points, including error paths and related configuration, resources, documentation, or translations
when the task or changed interface needs them. Distinguish an omitted requirement from an assumption about product
behavior. Do not infer that working happy-path code or checked checkboxes mean the whole task is implemented.

For schema or stored BLOB changes, follow the repository's migration guidance. Check DDL migrations, sequential schema
versions and flags, `AppSchemaVersions`/`AppDataSchemaVersion`, and both in-place and backup-based data migrations as
applicable. Verify migration helpers are shared, updaters are registered, and `getUpdatedVersion()` identifies the
version being migrated **from**. Review upgrade/restore behavior and its tests; a fresh-database test alone does not
establish compatibility with existing installations. Changes to persisted BLOB fields or their semantics require
the applicable data-schema migrations under current `AGENTS.md`; compatibility evidence helps assess their impact
and test coverage and does not waive that requirement. Distinguish persisted structures from transient DTOs.

Return an acceptance-criterion table with implementation evidence, test evidence, status, and any blocking questions,
as well as actionable bugs and regressions. Assess correctness independently from whether tests happen to pass.

## Code quality

Apply current `AGENTS.md` conventions to changed code. Use these decision criteria:

- Methods have one purpose and operate at one abstraction level. Identify concrete mixtures of orchestration and
  detail, unrelated responsibilities, or blocks whose extraction would make their purpose clear. Do not split code
  simply to meet an arbitrary method-size target.
- Names express domain intent at their scope; broad scopes deserve clear names and local names can be concise.
  Flag ambiguous names when they obscure actual behavior.
- Remove redundant narration, commented-out code, and comments that compensate for unclear names or structure.
  Keep comments needed to explain a non-obvious constraint, protocol rule, or reasoning.
- Organize new code by feature, with new packages under `io.imunity`. Avoid spreading one new feature across generic
  controller/service/repository packages or putting feature-specific helpers in shared utilities. Do not require
  relocating the entire legacy package structure to review a local change.
- Use the narrowest visibility: private, then package access, then wider access only for a real caller or framework
  contract. Inspect callers before proposing narrower access; framework, proxy, serialization, and public API needs
  can justify exposure. Prefer encapsulated state and avoid exposing mutable internals.
- Prefer immutable DTOs and configuration, and Java records where compatible with their contracts. Check defensive
  copies of collections, arrays, and nested values: a record is only shallowly immutable. Do not force records onto
  types with a necessary framework lifecycle or mutable domain responsibility.
- Check Java 21, tabs, new-line braces, 120-character lines, explicit organized imports, and unused imports where
  applicable. Prioritize changes that affect readability or maintenance over a large list of cosmetic observations.

Explain the concrete readability, coupling, or maintenance problem and an appropriately sized correction. Separate
low-priority recommendations from defects and do not propose unrelated refactors.

## Security

Identify affected actors, assets, input sources, and trust boundaries before looking for exploit paths. For relevant
changed paths, inspect authentication, authorization at the protected operation, privilege changes, ownership and
group/tenant isolation, and whether alternative entry points bypass checks. A UI control or authenticated session
does not by itself authorize a backend operation.

Trace attacker-controlled data through SQL, commands, templates/HTML, redirects, URLs, filesystem paths, XML or
deserialization, and outbound network calls where those sinks exist. Check encoding and validation at the right
boundary, safe parsing, request forgery/replay controls, protocol validation in SAML/OAuth/OIDC, tokens and sessions,
and relevant cryptographic handling. Check for secrets, sensitive logging/error output, excessive data exposure,
unsafe configuration defaults, and resource-exhaustion paths when introduced by the change.

A security finding needs an actor/input, reachable path, missing or ineffective control, and concrete impact.
Distinguish demonstrated vulnerabilities from hardening suggestions and incomplete evidence. Avoid hypothetical
warnings with no connection to the selected changes. Redact secret values in evidence. Request regression tests
for relevant denial cases and verify protection exists at the actual trust boundary.

## Common return format

For every candidate finding return priority, category, path/precise line and snapshot SHA, trigger, consequence,
evidence, relevant task criterion, and recommended correction. For missing tests, specify scenario and layer.
Also return inspected areas, relevant requirement/test mappings, validation requested or received, and limitations.
Say when no actionable finding is supported; do not manufacture one to fill the assignment.
