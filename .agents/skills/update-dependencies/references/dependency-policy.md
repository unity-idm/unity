# Dependency policy

Except for the Vaadin free-release requirement below, this policy affects only minor and major updates in full
mode. Patch/revision updates remain eligible for bulk processing.

## Vaadin free-release requirement

Use only freely usable Vaadin platform releases. This is a hard, unconditional requirement for patch, minor, major,
and dedicated Vaadin updates. Never select or retain a release that Vaadin identifies as commercial or that
requires a paid Vaadin license for building or running the application. Do not offer or accept an override.

Before proposing or applying a Vaadin version:

1. Fetch the official Vaadin platform release notes for the Maven candidate and other releases in the requested
   version line.
2. Check the linked Flow changelog for licensing changes. Do not infer that a release is free merely because its
   artifacts are available from Maven Central or declare an open-source license.
3. If Maven's newest candidate is commercial, walk backward through the release versions present in Maven-fetched
   metadata and select the newest release whose official notes do not identify it as commercial and whose linked
   changelogs do not activate paid license validation.
4. If no free candidate is newer than the current version, leave Vaadin unchanged. Report all newer commercial
   candidates as skipped and explain the licensing reason.

Treat a selected release found in Maven-fetched metadata as an exact Maven version for the skill's exact-version
guardrail. Always report the official release-note and changelog URLs used to establish that it is free.

## Manual-only non-patch updates

### Vaadin platform

- Match selectors: `vaadin.version`, `com.vaadin:vaadin-bom`, and `com.vaadin:vaadin-maven-plugin`.
- Policy: apply the Vaadin free-release requirement above, report available minor/major releases, leave them
  unchanged, and handle them in a separate dedicated manual upgrade.

Match primarily by the controlling version property, then by Maven coordinates. A dependency merely used by a
Vaadin module is not blacklisted unless it matches a selector above.

Keep this table narrow. Add another dependency only when the user identifies it as requiring dedicated manual
migration work.
