# Dependency policy

This policy affects only minor and major updates in full mode. Patch/revision updates remain eligible for bulk
processing.

## Manual-only non-patch updates

### Vaadin platform

- Match selectors: `vaadin.version`, `com.vaadin:vaadin-bom`, and `com.vaadin:vaadin-maven-plugin`.
- Policy: report available minor/major releases, leave them unchanged, and handle them with the dedicated staged
  workflow in [vaadin-updates.md](vaadin-updates.md). Do not offer a Vaadin minor or major update as an automatic
  full-mode choice.

Match primarily by the controlling version property, then by Maven coordinates. A dependency merely used by a
Vaadin module is not blacklisted unless it matches a selector above.

Keep this table narrow. Add another dependency only when the user identifies it as requiring dedicated manual
migration work.
