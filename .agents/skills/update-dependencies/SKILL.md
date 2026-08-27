---
name: update-dependencies
description: >-
  Update Maven dependency and build-plugin versions in the Unity root POM, either as one bulk patch/revision update
  or as an interactive full update with per-library minor/major choices. Use when asked to discover, apply, build,
  and commit dependency updates in a Unity checkout.
---

# Update Dependencies

Update dependency and build-plugin versions declared by the master `pom.xml`. Use Maven's Versions Plugin to
discover versions, make exact and reviewable POM edits, run the repository's full build, and commit only a
successful result.

## Choose the mode

- **Patch** or **revision** mode updates every eligible incremental version in bulk and creates one commit.
- **Full** mode includes the bulk patch pass, then asks which eligible libraries should receive a minor or major
  update before creating one commit.

Infer the mode from the request. If it is not explicit, ask the user to choose patch/revision or full mode before
changing files. Discovery may happen before that question because it is read-only.

For patch/revision mode, read [references/patch-mode.md](references/patch-mode.md). For full mode, read all of:

- [references/patch-mode.md](references/patch-mode.md)
- [references/full-mode.md](references/full-mode.md)
- [references/dependency-policy.md](references/dependency-policy.md)

For any Vaadin candidate in either mode, also read the **Vaadin free-release requirement** in
[references/dependency-policy.md](references/dependency-policy.md).

## Guardrails

- Work from the checkout root containing the master `pom.xml`; read and follow its `AGENTS.md` first.
- Inspect `git status --short` before editing. Preserve unrelated work and never stage it. If `pom.xml` already has
  user changes, inspect them and stop for clarification when the skill's edits cannot be isolated safely.
- Scope automated version edits to the root `pom.xml`. Use Maven with `-N` for version discovery so module POMs are
  not processed. Never change the Unity project version, reactor-module versions, or parent coordinates as part of
  dependency updating.
- Use release versions only. Honor the root POM's Versions Plugin configuration and `version-rules.xml`; do not
  bypass ignored-version rules merely to reach a newer number.
- For Vaadin, use only freely usable releases. Never select or retain a release that Vaadin identifies as
  commercial or that requires a paid Vaadin license for building or running the application. This requirement is
  unconditional, applies in every mode, and cannot be overridden by a request for the "latest" version. Resolve the
  newest free release as specified in `references/dependency-policy.md`.
- Treat one version property or BOM as one logical library even when it controls several artifacts. Deduplicate
  property, dependency, and plugin report entries before editing or prompting.
- Interpret patch/minor/major using Maven's version comparison and the existing version line. If a version scheme is
  ambiguous, do not guess; identify that candidate and ask for clarification.
- Apply only exact versions reported by Maven. Inspect `git diff -- pom.xml` after edits and remove any unintended
  changes before building.
- A dependency update can include the smallest source and test adaptations required by the selected versions, but
  do not expand into unrelated refactoring.

## Build and commit

Run the full repository build with tests after the complete update set is ready:

```bash
mvn -T 1C clean install -Dunity.selenium.opts=--headless=new -Dgpg.skip=true > "$dep_update_tmp/build.log" 2>&1
```

Create `dep_update_tmp` with `mktemp -d` earlier in the run. While the build runs, keep the user informed at least
once per minute. On failure, inspect the saved log, find the root cause, and fix update-caused compatibility or test
problems within scope. Do not commit while the required build fails. Preserve the log while it is useful, then
remove the verified temporary directory created for this run.

Before committing, run `git diff --check`, inspect the complete diff, and stage explicit attributable paths only;
never use `git add -A`. Create exactly one commit for the completed update set unless the user requested a different
split. Prefix the concise imperative commit subject with an issue key when one is applicable. If Maven finds no
eligible updates, do not build or create an empty commit; report that the POM is current for the selected mode.

Finish by reporting the old-to-new versions, skipped and manual-only candidates, build result, and commit hash.
