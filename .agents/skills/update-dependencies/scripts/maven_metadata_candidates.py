#!/usr/bin/env python3

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path


NUMERIC_VERSION = re.compile(r"^[0-9]+(?:\.[0-9]+)*$")


def parse_arguments() -> argparse.Namespace:
	parser = argparse.ArgumentParser(
		description="Classify numeric Maven plugin releases from locally cached metadata."
	)
	parser.add_argument("--repository", required=True, type=Path)
	parser.add_argument("--group-id", required=True)
	parser.add_argument("--artifact-id", required=True)
	parser.add_argument("--current", required=True)
	parser.add_argument("--rules", type=Path)
	return parser.parse_args()


def load_ignore_patterns(rules_path: Path | None) -> list[re.Pattern[str]]:
	if rules_path is None:
		return []

	root = ElementTree.parse(rules_path).getroot()
	patterns = []
	for element in root.findall(".//ignoreVersion"):
		pattern_type = element.get("type", "exact")
		if pattern_type != "regex":
			raise ValueError(f"Unsupported ignoreVersion type: {pattern_type}")
		if element.text:
			patterns.append(re.compile(element.text))
	return patterns


def load_versions(metadata_directory: Path) -> tuple[set[str], list[str]]:
	metadata_files = sorted(metadata_directory.glob("maven-metadata-*.xml"))
	versions = set()
	for metadata_file in metadata_files:
		root = ElementTree.parse(metadata_file).getroot()
		for version in root.findall("./versioning/versions/version"):
			if version.text:
				versions.add(version.text.strip())
	return versions, [str(path) for path in metadata_files]


def is_ignored(version: str, patterns: list[re.Pattern[str]]) -> bool:
	if "snapshot" in version.lower():
		return True
	return any(pattern.fullmatch(version) for pattern in patterns)


def numeric_parts(version: str) -> tuple[int, ...]:
	return tuple(int(part) for part in version.split("."))


def padded(parts: tuple[int, ...], width: int) -> tuple[int, ...]:
	return parts + (0,) * (width - len(parts))


def greatest(candidates: list[tuple[str, tuple[int, ...]]], width: int) -> str | None:
	if not candidates:
		return None
	return max(candidates, key=lambda candidate: padded(candidate[1], width))[0]


def classify(
	current: str,
	versions: set[str],
	ignore_patterns: list[re.Pattern[str]],
) -> dict[str, object]:
	if not NUMERIC_VERSION.fullmatch(current):
		raise ValueError(f"Current version is not purely numeric: {current}")

	allowed_versions = sorted(version for version in versions if not is_ignored(version, ignore_patterns))
	unclassified_versions = [version for version in allowed_versions if not NUMERIC_VERSION.fullmatch(version)]
	numeric_versions = [version for version in allowed_versions if NUMERIC_VERSION.fullmatch(version)]
	current_parts = numeric_parts(current)
	parsed_versions = [(version, numeric_parts(version)) for version in numeric_versions]
	width = max([len(current_parts), *(len(parts) for _, parts in parsed_versions)], default=len(current_parts))
	current_key = padded(current_parts, width)
	current_major = current_key[0]
	current_minor = current_key[1] if width > 1 else 0

	newer = [
		(version, parts)
		for version, parts in parsed_versions
		if padded(parts, width) > current_key
	]
	patch = [
		(version, parts)
		for version, parts in newer
		if padded(parts, width)[0] == current_major
		and (padded(parts, width)[1] if width > 1 else 0) == current_minor
	]
	minor = [
		(version, parts)
		for version, parts in newer
		if padded(parts, width)[0] == current_major
		and (padded(parts, width)[1] if width > 1 else 0) > current_minor
	]
	major = [
		(version, parts)
		for version, parts in newer
		if padded(parts, width)[0] > current_major
	]

	return {
		"current": current,
		"latest_patch": greatest(patch, width),
		"latest_minor": greatest(minor, width),
		"latest_major": greatest(major, width),
		"unclassified_allowed_versions": unclassified_versions,
	}


def main() -> int:
	arguments = parse_arguments()
	metadata_directory = (
		arguments.repository
		/ Path(*arguments.group_id.split("."))
		/ arguments.artifact_id
	)
	try:
		ignore_patterns = load_ignore_patterns(arguments.rules)
		versions, metadata_files = load_versions(metadata_directory)
		if not metadata_files:
			raise ValueError(f"No Maven metadata found in {metadata_directory}")
		result = classify(arguments.current, versions, ignore_patterns)
	except (ElementTree.ParseError, OSError, re.error, ValueError) as error:
		print(json.dumps({"error": str(error)}, indent=2))
		return 1

	result.update(
		{
			"coordinate": f"{arguments.group_id}:{arguments.artifact_id}",
			"metadata_files": metadata_files,
		}
	)
	print(json.dumps(result, indent=2))
	return 0


if __name__ == "__main__":
	sys.exit(main())
