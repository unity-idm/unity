# Git scope and shared validation

## Resolve once

Keep the supplied input names and immutable resolved SHAs in the context. State whether remote refs were refreshed;
the scope helper uses only locally available objects. Fetch a known remote only when necessary for the requested
scope or freshness, then resolve again before delegation. Do not silently call stale tracking data current.

Run the helper from the skill directory or use its absolute path, with properly quoted user values:

```bash
python3 scripts/resolve_scope.py --repo /path/to/unity --branch feature/example
python3 scripts/resolve_scope.py --repo /path/to/unity --target dev-4.2 --commits abc1234 def5678
```

The helper defaults target to `dev`. A bare branch name can resolve to a local branch or its `origin` tracking ref;
if both exist and differ, require an explicit choice such as `refs/heads/dev` or `refs/remotes/origin/dev`.
An explicitly qualified ref chooses that snapshot. Missing or ambiguous refs must be resolved before final review;
do not substitute a different branch. Record dirty working-tree state, but keep those changes outside review scope.
Heed shallow-history warnings: fetch needed history when ancestry or comparison parents are unavailable, then
freeze scope again. A shallow boundary is not a root commit. The helper stops when a selected comparison parent
is missing, rather than producing a misleading patch against an empty tree.

Read the JSON comparison entries emitted by the helper. Generate a canonical patch and changed-path list for each
using `git diff --no-ext-diff --no-textconv --find-renames <base> <head> --` and its `--name-status` variant. For a root
commit, use `git diff-tree --root --no-commit-id --no-ext-diff --no-textconv -r -p <head> --`, adding rename detection
as appropriate. Preserve deletion and rename evidence and inspect the relevant parent snapshot for deleted code.

### Branch mode

Freeze source and target SHAs and their unique merge base. The patch is **merge-base to source**, equivalent to a
three-dot diff with frozen SHAs; target-to-source comparison can misreport target-only work as source deletions.
Review the cumulative final change, with individual commits as context. Commits already present in target are not
new changes. If multiple merge bases exist, resolve the intended baseline explicitly rather than choosing one.
An empty branch diff can be a valid no-change result; report it without inventing review work.

### Explicit-commit mode

Freeze and deduplicate exactly the supplied commit SHAs. Inspect each against its parent; the union of those patches
is the selected scope. Never substitute the bounding range or target-to-last-commit diff for a noncontiguous set.
Inspect surrounding code as context, while keeping excluded commits outside the findings' attribution.

The helper accepts individual commit identifiers, not range expressions. If the user supplies a range, clarify its
intended semantics when needed and expand it once into an exact list before calling the helper. `A..B` excludes A
and selects commits reachable from B but not A; `A...B` is a symmetric difference, not a synonym. Record any expansion.

For a selected merge commit, require the mainline parent and pass `--mainline N`. The comparison is against that
parent, not a combined diff that may omit changes. If selected merges need different parents, resolve them separately
and combine the frozen manifest entries. Do not assume first parent merely because it is conventional.

Assess the final effect of the selected changes; a later selected fix can resolve an earlier selected defect.
Selected patches may overlap, cancel, depend on excluded commits, or already exist in the target. Report relevant
prerequisite/application issues separately from confirmed defects. Do not cherry-pick into the user's working tree.

## Choose the tested snapshot

Use the user's current checkout only if it exactly matches the intended committed snapshot and has no local changes
affecting the check. Otherwise use a temporary detached worktree or an isolated clone. Preserve the user's checkout,
index, branch, and local files. The coordinator prepares the snapshot and runs Maven once; reviewers share evidence.

- **Branch:** run checks at the frozen source SHA. State if target integration was tested separately; a source build
  does not establish merge compatibility with a newer target.
- **Selected commits:** a checkout at the last selected SHA may include excluded commits. Such tests validate that
  containing snapshot, not exactly the selected set. For exact target-application evidence, use an isolated worktree
  at the frozen target and apply only selected commits in dependency order with the declared merge mainline. Stop on
  conflicts or missing prerequisites, preserve diagnostics, and do not silently expand the scope or repair code.
  For commits on unrelated histories with no unambiguous application order, clarify intent before synthesizing a tree.

Label every check with its SHA or synthesized-tree provenance. Do not report selected changes as passing solely
because a containing snapshot with excluded fixes passed. Test-only reproductions may be created in an isolated
temporary location; they are review evidence and must not become unrequested repository edits.

## Run relevant checks

Read current POMs and repository instructions before choosing commands. Unity uses Java 21 and Maven; unit tests
live beside modules, cross-module/browser tests in `integration-tests/`, and JaCoCo supports coverage analysis.
Test classification depends on exercised behavior and real dependencies, not these locations alone.

Start with affected unit tests and modules; include relevant integration tests for changed wiring, persistence,
serialization, or protocol boundaries. Example commands, run at the tested snapshot:

```bash
mvn -pl engine -am test -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
mvn -pl integration-tests -am test '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

Select the actual affected modules/classes, rather than copying these examples indiscriminately. If using
`-Dtest=SomeTest` with `-am`, upstream modules without matching tests may need
`-Dsurefire.failIfNoSpecifiedTests=false`; inspect Surefire reports to confirm intended tests were discovered and run.
Do not equate a zero-test success with validation. Inspect matching-snapshot JaCoCo reports if available.

Use broader builds when the change's cross-module or compatibility impact warrants them. Full build commands:

```bash
review_tmp=$(mktemp -d)
mvn -T 1C clean install '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > "$review_tmp/build.log" 2>&1
mvn -T 1C -DskipTests=false verify '-Dunity.selenium.opts=--headless=new,--disable-dev-shm-usage,--disable-gpu' -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost= > "$review_tmp/verify.log" 2>&1
```

These are alternatives chosen for the validation needed, not a requirement to repeat equivalent suites. All full
builds, including any justified build without tests, must redirect output to a log. Inspect failures and distinguish
change-caused defects from baseline/environment failures, using a baseline check only when necessary. Diagnose and
report review defects; fixing the reviewed implementation requires a separate user request. Keep useful logs until
diagnostics are captured, then remove only owned temporary files/worktrees. Preserve unrelated user files.

Record commands, exit status, actual suites/test counts, snapshot provenance, and skipped or unavailable checks.
Keep the user informed during long builds. Browser tests in containers need all three options above; clearing
Java proxy-host properties prevents localhost TLS/WebSocket tests from being routed through environment proxies.
