"""Behavior checks using disposable real Git repositories."""

import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from resolve_scope import ScopeError, resolve_scope


class ResolveScopeTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repository = Path(self.temporary.name)
        self.git("init", "-q", "-b", "dev")
        self.git("config", "user.name", "Review Test")
        self.git("config", "user.email", "review@example.invalid")
        self.root = self.commit("initial.txt", "initial")

    def git(self, *arguments):
        return subprocess.run(["git", "-C", str(self.repository), *arguments],
                              text=True, capture_output=True, check=True).stdout.strip()

    def commit(self, filename, content):
        (self.repository / filename).write_text(content, encoding="utf-8")
        self.git("add", "--", filename)
        self.git("commit", "-q", "-m", content)
        return self.git("rev-parse", "HEAD")

    def test_should_compare_branch_from_divergence_without_target_changes(self):
        self.git("branch", "feature")
        target = self.commit("target.txt", "target-only")
        self.git("checkout", "-q", "feature")
        feature = self.commit("feature.txt", "feature-only")

        scope = resolve_scope(self.repository, branch="feature")

        self.assertEqual(scope["target"]["sha"], target)
        self.assertEqual(scope["merge_base"], self.root)
        self.assertEqual(scope["commits"], [feature])
        comparison = scope["comparisons"][0]
        self.assertEqual((comparison["base"], comparison["head"]), (self.root, feature))
        self.assertEqual(self.git("diff", "--name-only", comparison["base"], comparison["head"]),
                         "feature.txt")

    def test_should_use_version_target(self):
        self.git("checkout", "-q", "-b", "dev-4.5")
        version = self.commit("version.txt", "version")
        self.git("checkout", "-q", "-b", "feature")
        feature = self.commit("feature.txt", "feature")

        scope = resolve_scope(self.repository, target="dev-4.5", branch="feature")

        self.assertEqual(scope["target"]["sha"], version)
        self.assertEqual(scope["merge_base"], version)
        self.assertEqual(scope["commits"], [feature])

    def test_should_keep_noncontiguous_selection_exact_and_order_ancestors_first(self):
        self.git("checkout", "-q", "-b", "feature")
        first = self.commit("first.txt", "first")
        excluded = self.commit("excluded.txt", "excluded")
        last = self.commit("last.txt", "last")

        scope = resolve_scope(self.repository, commits=[last, first, last])

        self.assertEqual(scope["commits"], [last, first])
        self.assertEqual(scope["application_order"], [first, last])
        self.assertEqual([entry["head"] for entry in scope["comparisons"]], [first, last])
        self.assertEqual(scope["comparisons"][1]["base"], excluded)
        self.assertTrue(any(excluded in warning for warning in scope["warnings"]))

    def test_should_require_and_validate_merge_mainline(self):
        self.git("checkout", "-q", "-b", "feature")
        feature = self.commit("feature.txt", "feature")
        self.git("checkout", "-q", "dev")
        target = self.commit("target.txt", "target")
        self.git("merge", "-q", "--no-ff", "feature", "-m", "merge")
        merge = self.git("rev-parse", "HEAD")

        with self.assertRaisesRegex(ScopeError, "requires --mainline"):
            resolve_scope(self.repository, commits=[merge])
        with self.assertRaisesRegex(ScopeError, "exceeds parent count"):
            resolve_scope(self.repository, commits=[merge], mainline=3)
        first = resolve_scope(self.repository, commits=[merge], mainline=1)
        second = resolve_scope(self.repository, commits=[merge], mainline=2)

        self.assertEqual(first["comparisons"][0]["base"], target)
        self.assertEqual(second["comparisons"][0]["base"], feature)
        self.assertEqual(second["comparisons"][0]["mainline"], 2)
        branch_scope = resolve_scope(self.repository, target=self.root, branch="dev")
        self.assertIn(merge, branch_scope["commits"])

    def test_should_reject_multiple_merge_bases(self):
        left = self.commit("left.txt", "left")
        self.git("checkout", "-q", "-b", "right", self.root)
        right = self.commit("right.txt", "right")
        tree = self.git("rev-parse", "HEAD^{tree}")
        first_merge = self.git("commit-tree", tree, "-p", left, "-p", right, "-m", "first merge")
        second_merge = self.git("commit-tree", tree, "-p", right, "-p", left, "-m", "second merge")
        self.git("update-ref", "refs/heads/dev", first_merge)
        self.git("branch", "feature", second_merge)

        with self.assertRaisesRegex(ScopeError, "Multiple merge-bases"):
            resolve_scope(self.repository, branch="feature")

    def test_should_reject_unrelated_branch_history(self):
        tree = self.git("rev-parse", "HEAD^{tree}")
        unrelated = self.git("commit-tree", tree, "-m", "unrelated root")
        self.git("branch", "unrelated", unrelated)

        with self.assertRaisesRegex(ScopeError, "no usable common ancestor"):
            resolve_scope(self.repository, branch="unrelated")

    def test_should_represent_root_as_empty_tree_and_warn_reachable(self):
        scope = resolve_scope(self.repository, commits=[self.root])

        self.assertIsNone(scope["comparisons"][0]["base"])
        self.assertEqual(scope["comparisons"][0]["head"], self.root)
        self.assertTrue(any("already reachable" in warning for warning in scope["warnings"]))

    def test_should_reject_divergent_local_and_remote_shorthand(self):
        newer = self.commit("newer.txt", "newer")
        self.git("update-ref", "refs/remotes/origin/dev", self.root)

        with self.assertRaisesRegex(ScopeError, "Ambiguous reference"):
            resolve_scope(self.repository, branch="dev")
        local = resolve_scope(self.repository, target="refs/heads/dev", commits=[self.root])
        remote = resolve_scope(self.repository, target="refs/remotes/origin/dev", commits=[self.root])

        self.assertEqual(local["target"]["sha"], newer)
        self.assertEqual(remote["target"]["sha"], self.root)

    def test_should_accept_equal_local_remote_and_remote_only_default(self):
        self.git("update-ref", "refs/remotes/origin/dev", self.root)
        scope = resolve_scope(self.repository, commits=[self.root])
        self.assertEqual(scope["target"]["resolved_ref"], "refs/heads/dev")
        self.git("branch", "-m", "local")

        scope = resolve_scope(self.repository, commits=[self.root])

        self.assertEqual(scope["target"]["resolved_ref"], "refs/remotes/origin/dev")

    def test_should_preserve_head_and_pseudoref_semantics_before_remote_shorthand(self):
        target = self.commit("target.txt", "target")
        self.git("update-ref", "refs/remotes/origin/dev", target)
        self.git("symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/dev")
        self.git("checkout", "-q", "-b", "feature", self.root)
        first = self.commit("first.txt", "first")
        last = self.commit("last.txt", "last")
        self.git("update-ref", "ORIG_HEAD", first)
        self.git("update-ref", "refs/remotes/origin/ORIG_HEAD", target)

        branch = resolve_scope(self.repository, branch="HEAD")
        selected = resolve_scope(self.repository, commits=["HEAD~1", "ORIG_HEAD", "@"])

        self.assertEqual(branch["source"]["sha"], last)
        self.assertEqual(branch["source"]["resolved_ref"], "HEAD")
        self.assertEqual(branch["merge_base"], self.root)
        self.assertEqual(selected["commits"], [first, last])

    def test_should_fail_missing_target_without_main_fallback(self):
        self.git("branch", "-m", "main")
        with self.assertRaisesRegex(ScopeError, "Missing or invalid.*dev"):
            resolve_scope(self.repository, branch="main")

    def test_should_leave_dirty_checkout_index_and_refs_untouched(self):
        self.git("checkout", "-q", "-b", "feature")
        self.commit("feature.txt", "feature")
        (self.repository / "initial.txt").write_text("modified", encoding="utf-8")
        (self.repository / "untracked.txt").write_text("untracked", encoding="utf-8")
        before = self.git("status", "--porcelain=v1")
        refs = self.git("show-ref")
        index = (self.repository / ".git" / "index").read_bytes()

        scope = resolve_scope(self.repository, branch="feature")

        self.assertTrue(scope["worktree_dirty"])
        self.assertEqual((self.repository / ".git" / "index").read_bytes(), index)
        self.assertEqual(self.git("show-ref"), refs)
        self.assertEqual(self.git("status", "--porcelain=v1"), before)

    def test_should_reject_missing_or_unsafe_commit_references(self):
        for reference in ("missing", "--all", "dev bogus", "", "dev\x00"):
            with self.subTest(reference=reference):
                with self.assertRaises(ScopeError):
                    resolve_scope(self.repository, commits=[reference])

    def test_should_reject_commit_ranges_instead_of_expanding_them(self):
        last = self.commit("last.txt", "last")
        for reference in (f"{self.root}..{last}", f"{self.root}...{last}"):
            with self.subTest(reference=reference):
                with self.assertRaisesRegex(ScopeError, "ranges are unsupported"):
                    resolve_scope(self.repository, commits=[reference])

    def test_should_not_treat_shallow_boundary_as_root(self):
        boundary = self.commit("boundary.txt", "boundary")
        shallow_directory = tempfile.TemporaryDirectory()
        self.addCleanup(shallow_directory.cleanup)
        subprocess.run(["git", "clone", "--quiet", "--depth", "1", self.repository.as_uri(),
                        shallow_directory.name], text=True, capture_output=True, check=True)
        parents = subprocess.run(["git", "-C", shallow_directory.name, "rev-list", "--parents",
                                  "-n", "1", boundary], text=True, capture_output=True, check=True)
        self.assertEqual(parents.stdout.strip(), boundary)

        with self.assertRaisesRegex(ScopeError, "fetch required history"):
            resolve_scope(shallow_directory.name, commits=[boundary])
        branch_scope = resolve_scope(shallow_directory.name, branch="dev")
        self.assertTrue(any("shallow" in warning for warning in branch_scope["warnings"]))

    def test_should_emit_json_and_reject_invalid_cli_combinations(self):
        script = Path(__file__).with_name("resolve_scope.py")
        command = [sys.executable, str(script), "--repo", str(self.repository)]
        valid = subprocess.run(command + ["--commits", self.root], text=True, capture_output=True)
        self.assertEqual(valid.returncode, 0, valid.stderr)
        self.assertEqual(json.loads(valid.stdout)["commits"], [self.root])
        for arguments in ([], ["--branch", "dev", "--commits", self.root],
                          ["--commits"], ["--branch", "dev", "--mainline", "1"],
                          ["--commits", self.root, "--mainline", "0"],
                          ["--branch=--all"], ["--commits", "--all"],
                          ["--commits", "missing"], ["--commits", "dev..dev"],
                          ["--commits", "dev...dev"]):
            with self.subTest(arguments=arguments):
                result = subprocess.run(command + arguments, text=True, capture_output=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(result.stdout, "")
                self.assertIn("error:", result.stderr)


if __name__ == "__main__":
    unittest.main()
