# Patch/revision mode

Patch mode updates all eligible incremental versions in the master POM as one reviewed change and one commit.

## Discover candidates

Create a temporary directory outside the checkout:

```bash
dep_update_tmp=$(mktemp -d)
```

Run all three bounded reports from the checkout root. Redirecting each report makes candidate comparison reliable
and keeps verbose Maven output out of the conversation.

```bash
mvn -N versions:display-property-updates \
	-DallowMajorUpdates=false -DallowMinorUpdates=false -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/properties-patch.log" 2>&1
mvn -N versions:display-dependency-updates \
	-DallowMajorUpdates=false -DallowMinorUpdates=false -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/dependencies-patch.log" 2>&1
mvn -N versions:display-plugin-updates \
	-DallowSnapshots=false > "$dep_update_tmp/plugins-patch.log" 2>&1
```

Check every command's exit status and inspect failed reports before continuing. Read the three reports and map each
candidate back to its version property or literal `<version>` in the root `pom.xml`. Exclude reactor/project/parent
versions and deduplicate libraries controlled by a shared property or BOM.

Unlike the other two reports, `display-plugin-updates` exposes only the latest release and does not honor
major/minor bounds. For each literal plugin version not already controlled by a reported property, use the bundled
`scripts/maven_metadata_candidates.py` to classify the metadata that the plugin report just fetched:

```bash
maven_local_repo=$(mvn -q help:evaluate -Dexpression=settings.localRepository -DforceStdout 2>/dev/null)
python3 <resolved-skill-path>/scripts/maven_metadata_candidates.py \
	--repository "$maven_local_repo" \
	--group-id org.apache.maven.plugins \
	--artifact-id maven-compiler-plugin \
	--current 3.14.1 \
	--rules version-rules.xml
```

Use the actual coordinate and current version for each plugin. Only `latest_patch` belongs in the patch set. If the
script reports `unclassified_allowed_versions`, do not silently treat them as older; compare them using Maven's
rules or flag the plugin as ambiguous and ask the user.

The non-patch blacklist does not apply here: a blacklisted library may receive a patch update.

## Apply the patch set

Update all discovered patch versions together with precise POM edits. Do not replace version ranges, introduce
properties, or reorganize the POM as part of this pass. Re-run the same reports after editing; there should be no
remaining eligible property/dependency patch candidate unless the plugin metadata changed during the run. Re-run
the metadata classifier for changed literal plugins. Resolve unexpected leftovers before building.

Review the old-to-new list and `git diff -- pom.xml`, then follow the build and commit procedure in `SKILL.md`. The
single commit includes every successful patch update plus any narrowly required compatibility/test changes.
