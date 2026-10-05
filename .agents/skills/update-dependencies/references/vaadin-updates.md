# Vaadin updates

Use this workflow for the root `vaadin.version`, `com.vaadin:vaadin-bom`, and
`com.vaadin:vaadin-maven-plugin`. The shared platform version is one logical dependency even though it controls
artifacts across many modules.

## Choose exactly one version hop

For a current stable version `MAJOR.MINOR.PATCH`, define `MAX_FREE` as the greatest stable patch in an exact
`MAJOR.MINOR` line that Maven metadata contains and the official Vaadin sources establish as freely usable. Exclude
pre-releases.

- A patch update targets `MAJOR.MINOR.MAX_FREE`.
- A minor update targets `MAJOR.(MINOR + 1).MAX_FREE`. Never skip a minor number. If the requested destination is
  more than one minor away, handle each intervening minor as a separate hop in ascending order.
- A major update targets `(MAJOR + 1).0.MAX_FREE`. Never jump over a major or target a later minor of the new major.
  Before the major hop, the checkout must be on `MAJOR.LATEST_MINOR.MAX_FREE`, where `LATEST_MINOR` is the highest
  published stable minor number of the previous major. Reach it through the sequential minor hops above. If that
  latest minor has no free stable release, stop: an earlier free minor does not satisfy the major-upgrade
  prerequisite.

Complete the documentation review, compatibility plan, code changes, full build, and commit for one hop before
starting another. Stop the sequence when a hop has no free release or cannot be made to build; do not skip it to
reach a later line.

## Read the official material before editing

For every minor hop, the first implementation step is to fetch the official platform release and upgrade notes for
the target minor's `.0` release:

```text
https://github.com/vaadin/platform/releases/tag/MAJOR.MINOR.0
```

Read the release body, its `Full.Release.Note.md` asset when present, and every linked upgrading note or guide that
can affect this checkout. The platform release page is the source of truth for the component versions and links;
do not construct component tags from the platform version when the page supplies a different tag.

For a major hop, first read the target major's upgrade guide and follow its prerequisites and linked migration
instructions before reading the target `MAJOR.0.0` release material. For example, an upgrade to Vaadin 24 starts at:

```text
https://vaadin.com/docs/v24/upgrading
```

Use the equivalent `/docs/v<target-major>/upgrading` guide for other majors. Cover required Java, Spring, Servlet,
Node, build-tool, browser, theme, API, and configuration changes that apply to the checkout.

## Fetch every relevant changelog

The checklist below is complete for the Vaadin surfaces used by this checkout. Fetch every entry for the target
minor from the links in the platform release notes, even when two entries resolve to the same release page. Use the
target `.0` page and the selected `MAX_FREE` page when they differ, and review the cumulative changes over the whole
current-to-target range rather than only the last patch's delta:

1. **Vaadin Platform** release notes and the full release-note asset.
2. **Flow**, including server/runtime/client changes.
3. **Spring add-on** (`vaadin-spring`). Its platform entry may link to the Flow release; still inspect changes and
   comparisons affecting the `vaadin-spring` module.
4. **Maven plugin** (`com.vaadin:vaadin-maven-plugin` and its Flow plugin implementation). Inspect the platform
   Maven-plugin entry plus Flow release/compare changes affecting `flow-plugins/flow-maven-plugin`. When the platform
   entry has no direct link, use the full platform release note and the linked Flow changelog/compare; do not treat
   the missing link as evidence that the plugin has no relevant changes.
5. **Components**, covering both **Flow Components** Java wrappers and **Web Components** browser implementations.

Within the component changelogs, map all changed component sections to this checkout by searching Java imports
under `com.vaadin.flow.component` and frontend imports under `@vaadin`. Also inspect changes marked for all
components. Do not omit Web Components merely because the application normally uses their Flow wrappers. Fetch an
additional linked Vaadin changelog if the checkout begins using another platform surface; do not fetch unrelated
Hilla, CDI, Gradle-plugin, TestBench, or commercial-kit changelogs unless repository usage is found.

## Select the latest free patch

Use the Maven-fetched metadata for `com.vaadin:vaadin-bom` to list stable candidates in the exact target minor.
Starting with the newest candidate and walking backward:

1. Fetch that candidate's official platform release notes and linked Flow changelog.
2. Reject it if Vaadin identifies the release as commercial or if the notes or linked changelogs enable paid
   license validation for building or running the application.
3. Select the first remaining candidate. Availability from Maven Central or an open-source license declaration is
   not sufficient evidence that the release is free.

Never select an older patch while a newer verified-free patch exists. If no free candidate exists in the required
target line, leave Vaadin unchanged and stop the requested sequence. Report the candidates rejected as commercial
and the official URLs used for the decision. A verified candidate from Maven metadata is an exact Maven version for
the skill's exact-version guardrail.

## Plan and implement compatibility work

Before changing the version, turn the release notes, upgrade notes, and changelogs into a concrete impact list. For
each potentially relevant upstream change, record the affected code, configuration, theme, frontend, test, or build
usage and the required adaptation. Mark a change not applicable only after checking the repository for the affected
API or feature.

Update the root `vaadin.version` to the selected `MAX_FREE` patch and make the smallest required source,
configuration, and test changes from the impact list. Add or update focused tests for changed behavior. Review the
complete diff, then run the full build and commit procedure from `SKILL.md` for this hop. The final report for every
hop must include:

- old and new Vaadin versions;
- all platform, upgrade-guide, and required changelog URLs reviewed;
- the impact list and implemented compatibility changes;
- commercial releases skipped while determining `MAX_FREE`;
- full-build result and commit hash.
