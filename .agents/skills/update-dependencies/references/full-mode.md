# Full mode

Full mode performs the patch/revision pass first, then offers non-blacklisted minor and major updates to the user.
Do not apply a non-patch update without the user's explicit selection.

## Establish the patched baseline

Follow `patch-mode.md` through candidate discovery and POM editing, but defer the build and commit. Use that updated
POM as the baseline for minor/major discovery.

## Discover minor and major candidates

Run property and dependency reports twice. The minor-bounded reports keep the current major version; the unbounded
reports find the latest permitted release.

```bash
mvn -N versions:display-property-updates \
	-DallowMajorUpdates=false -DallowMinorUpdates=true -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/properties-minor.log" 2>&1
mvn -N versions:display-dependency-updates \
	-DallowMajorUpdates=false -DallowMinorUpdates=true -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/dependencies-minor.log" 2>&1

mvn -N versions:display-property-updates \
	-DallowMajorUpdates=true -DallowMinorUpdates=true -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/properties-major.log" 2>&1
mvn -N versions:display-dependency-updates \
	-DallowMajorUpdates=true -DallowMinorUpdates=true -DallowIncrementalUpdates=true \
	-DallowSnapshots=false > "$dep_update_tmp/dependencies-major.log" 2>&1
```

Check every exit status. For each literal plugin version, reuse `maven_metadata_candidates.py` as described in
`patch-mode.md`; its `latest_minor` and `latest_major` fields supply the full-mode choices. Correlate and deduplicate
all results by logical library/property:

- A release above the patched baseline but within its current major line is the latest **minor** choice.
- A release with a higher major component is the latest **major** choice.
- Do not show a choice when the report target is equal to the patched baseline.
- For non-semantic or otherwise ambiguous version lines, report the candidate separately and ask instead of
  classifying it automatically.

## Ask for selections

Apply `dependency-policy.md` before prompting. Put blacklist matches in a separate **manual-only** list with current
and available versions; do not offer them as automatic full-mode choices.

For every remaining logical library with a non-patch update, present one compact table containing its key/name,
patched current version, latest minor when available, and latest major when available. Ask the user to choose:

- `skip`
- `minor` when a newer same-major release exists
- `major` when a newer major release exists

Allow a concise grouped response such as “minor for all except Jackson; major for JUnit.” If there are many rows,
batch the questions without losing the per-library choices. Wait for the answer before editing any non-patch version.

## Apply and verify

Edit each chosen property or literal version to the exact reported target. Keep skipped and manual-only libraries
unchanged beyond the already applied patch pass. After any necessary compatibility work, run the full build once for
the complete selected set and create the single commit described in `SKILL.md`.
