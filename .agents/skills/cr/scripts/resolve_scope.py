#!/usr/bin/env python3
"""Freeze committed review scope without changing Git refs or the checkout."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys


class ScopeError(ValueError):
    pass


class Git:
    def __init__(self, repository):
        self.repository = str(Path(repository).resolve())
        self.environment = dict(os.environ, GIT_OPTIONAL_LOCKS="0")
        self.repository = self.run("rev-parse", "--show-toplevel").strip()

    def command(self, *arguments):
        try:
            return subprocess.run(
                ["git", "-C", self.repository, *arguments],
                env=self.environment, text=True, capture_output=True, check=False,
            )
        except OSError as error:
            raise ScopeError(str(error)) from error

    def run(self, *arguments):
        result = self.command(*arguments)
        if result.returncode:
            raise ScopeError(result.stderr.strip() or "Git command failed")
        return result.stdout

    def commit(self, reference):
        if (not reference or reference.startswith("-") or "\x00" in reference
                or any(c.isspace() for c in reference)):
            raise ScopeError(f"Invalid reference: {reference!r}")
        if ".." in reference:
            raise ScopeError("Commit ranges are unsupported; supply individual commit references")
        revision_base = reference.split("~", 1)[0].split("^", 1)[0].split("@{", 1)[0]
        pseudorefs = {"HEAD", "FETCH_HEAD", "ORIG_HEAD", "MERGE_HEAD", "CHERRY_PICK_HEAD",
                      "REVERT_HEAD", "REBASE_HEAD", "BISECT_HEAD", "AUTO_MERGE", "@"}
        if reference.startswith(("refs/", "@{")) or revision_base in pseudorefs:
            candidates = [reference]
        else:
            candidates = [f"refs/heads/{reference}", f"refs/remotes/{reference}",
                          f"refs/remotes/origin/{reference}", f"refs/tags/{reference}"]
        matches = []
        for candidate in candidates:
            result = self.command("rev-parse", "--verify", "--quiet", "--end-of-options",
                                  candidate + "^{commit}")
            if result.returncode == 0:
                matches.append((candidate, result.stdout.strip()))
        if len({sha for _, sha in matches}) > 1:
            refs = ", ".join(reference for reference, _ in matches)
            raise ScopeError(f"Ambiguous reference {reference!r}: {refs}; use a full ref")
        if matches:
            canonical, sha = matches[0]
        else:
            result = self.command("rev-parse", "--verify", "--quiet", "--end-of-options",
                                  reference + "^{commit}")
            if result.returncode:
                raise ScopeError(f"Missing or invalid commit reference: {reference!r}")
            canonical, sha = reference, result.stdout.strip()
        return {"ref": reference, "resolved_ref": canonical, "sha": sha}

    def ancestor(self, ancestor, descendant):
        result = self.command("merge-base", "--is-ancestor", ancestor, descendant)
        if result.returncode not in (0, 1):
            raise ScopeError(result.stderr.strip() or "Cannot determine commit ancestry")
        return result.returncode == 0


def resolve_scope(repository=".", target="dev", branch=None, commits=None, mainline=None):
    if bool(branch) == bool(commits):
        raise ScopeError("Supply exactly one branch or a nonempty commit selection")
    if mainline is not None and (mainline < 1 or branch is not None):
        raise ScopeError("--mainline must be positive and applies only to selected commits")
    git = Git(repository)
    target_ref = git.commit(target)
    scope = {
        "schema_version": 1, "repository": git.repository, "target": target_ref,
        "mode": "branch" if branch else "commits", "source": None, "merge_base": None,
        "commits": [], "application_order": [], "comparisons": [], "warnings": [],
        "worktree_dirty": bool(git.run("status", "--porcelain=v1", "-z")),
    }
    if git.run("rev-parse", "--is-shallow-repository").strip() == "true":
        scope["warnings"].append(
            "Repository is shallow; available history may omit branch common ancestors")
    if branch:
        source = git.commit(branch)
        result = git.command("merge-base", "--all", target_ref["sha"], source["sha"])
        bases = result.stdout.splitlines()
        if result.returncode or not bases:
            raise ScopeError("Target and source have no usable common ancestor")
        if len(bases) != 1:
            raise ScopeError("Multiple merge-bases; supply an unambiguous review scope")
        scope["source"], scope["merge_base"] = source, bases[0]
        scope["commits"] = git.run("rev-list", "--reverse", "--topo-order", source["sha"],
                                   "--not", target_ref["sha"]).splitlines()
        scope["application_order"] = list(scope["commits"])
        scope["comparisons"] = [{"base": bases[0], "head": source["sha"], "mainline": None}]
        if not scope["commits"]:
            scope["warnings"].append("Source has no commits absent from target")
        return scope

    scope["commits"] = list(dict.fromkeys(git.commit(ref)["sha"] for ref in commits))
    selected = set(scope["commits"])
    ancestry = git.run("rev-list", "--reverse", "--topo-order", *scope["commits"]).splitlines()
    scope["application_order"] = [sha for sha in ancestry if sha in selected]
    for sha in scope["application_order"]:
        headers = git.run("cat-file", "commit", sha).split("\n\n", 1)[0].splitlines()
        parents = [header.split()[1] for header in headers if header.startswith("parent ")]
        if len(parents) > 1:
            if mainline is None:
                raise ScopeError(f"Merge commit {sha} requires --mainline")
            if mainline > len(parents):
                raise ScopeError(f"--mainline {mainline} exceeds parent count for {sha}")
            base, parent_number = parents[mainline - 1], mainline
        else:
            base, parent_number = (parents[0] if parents else None), None
        if base and git.command("cat-file", "-e", base + "^{commit}").returncode:
            raise ScopeError(
                f"Required parent commit {base} is unavailable; fetch required history before review")
        scope["comparisons"].append({"base": base, "head": sha, "mainline": parent_number})
        if git.ancestor(sha, target_ref["sha"]):
            scope["warnings"].append(f"Selected commit {sha} is already reachable from target")
        if base and base not in selected and not git.ancestor(base, target_ref["sha"]):
            scope["warnings"].append(
                f"Commit {sha} has unselected parent {base} absent from target; "
                "patches or tests may need excluded prerequisites")
    scope["warnings"].append(
        "A checkout at a selected commit includes its ancestors, including unselected commits; "
        "tests there verify that snapshot, not necessarily only the selected changes")
    return scope


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".")
    parser.add_argument("--target", default="dev")
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--branch")
    selection.add_argument("--commits", nargs="+")
    parser.add_argument("--mainline", type=int)
    arguments = parser.parse_args()
    try:
        scope = resolve_scope(arguments.repo, arguments.target, arguments.branch,
                              arguments.commits, arguments.mainline)
    except ScopeError as error:
        parser.error(str(error))
    json.dump(scope, sys.stdout, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
