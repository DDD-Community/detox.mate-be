#!/usr/bin/env python3
"""Require final GREEN verification when product or test code changed."""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path


FINAL_RECORD = Path("_workspace/08_orchestrator_final-verification.md")
WATCHED_PREFIXES = ("src/main/", "src/test/")


def repo_root() -> Path:
    override = os.environ.get("DETOXMATE_HARNESS_ROOT")
    if override:
        return Path(override).resolve()

    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        cwd=Path.cwd(),
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode == 0 and result.stdout.strip():
        return Path(result.stdout.strip()).resolve()
    return Path.cwd().resolve()


def git_lines(root: Path, args: list[str]) -> list[str]:
    result = subprocess.run(
        ["git", *args],
        cwd=root,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        return []
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def changed_watched_paths(root: Path) -> list[str]:
    paths: set[str] = set()
    paths.update(git_lines(root, ["diff", "--name-only", "HEAD", "--", *WATCHED_PREFIXES]))
    paths.update(git_lines(root, ["ls-files", "--others", "--exclude-standard", "--", *WATCHED_PREFIXES]))
    return sorted(path for path in paths if path.startswith(WATCHED_PREFIXES))


def record_is_fresh(root: Path, changed_paths: list[str]) -> tuple[bool, str]:
    record_path = root / FINAL_RECORD
    if not record_path.exists():
        return False, f"missing {FINAL_RECORD.as_posix()}"

    existing_changed_paths = [root / path for path in changed_paths if (root / path).exists()]
    if not existing_changed_paths:
        return True, "fresh"

    newest_change = max(path.stat().st_mtime for path in existing_changed_paths)
    if record_path.stat().st_mtime < newest_change:
        return False, f"{FINAL_RECORD.as_posix()} is older than changed source or test files"
    return True, "fresh"


def command_passed(lines: list[str], pattern: re.Pattern[str]) -> bool:
    pass_markers = ("passed", "success", "succeeded", "green", "ok")
    fail_markers = ("failed", "failure", "error", "non-zero", "not run", "skipped")

    for index, line in enumerate(lines):
        if not pattern.search(line):
            continue
        window = " ".join(lines[index : index + 4])
        has_pass = any(marker in window for marker in pass_markers)
        has_fail = any(marker in window for marker in fail_markers)
        if has_pass and not has_fail:
            return True
    return False


def final_record_is_valid(root: Path) -> tuple[bool, str]:
    record_path = root / FINAL_RECORD
    text = record_path.read_text(encoding="utf-8", errors="replace").lower()
    lines = [line.strip() for line in text.splitlines() if line.strip()]

    required = {
        "./gradlew test": re.compile(r"(?:\./)?gradlew\s+test(?::|\s|$)"),
        "./gradlew clean build": re.compile(r"(?:\./)?gradlew\s+clean\s+build(?::|\s|$)"),
        "git diff --check": re.compile(r"git\s+diff\s+--check(?::|\s|$)"),
    }

    missing = [
        command
        for command, pattern in required.items()
        if not command_passed(lines, pattern)
    ]
    if missing:
        return False, f"{FINAL_RECORD.as_posix()} is missing passed results for: {', '.join(missing)}"
    return True, "valid"


def main() -> int:
    root = repo_root()
    changed_paths = changed_watched_paths(root)
    if not changed_paths:
        return 0

    is_fresh, freshness_reason = record_is_fresh(root, changed_paths)
    if not is_fresh:
        print("Detoxmate final GREEN hook blocked completion.", file=sys.stderr)
        print("src/main/** or src/test/** changed, so final verification must be recorded.", file=sys.stderr)
        print(f"Reason: {freshness_reason}", file=sys.stderr)
        print("Changed watched paths:", file=sys.stderr)
        for path in changed_paths:
            print(f"- {path}", file=sys.stderr)
        return 1

    is_valid, validity_reason = final_record_is_valid(root)
    if is_valid:
        return 0

    print("Detoxmate final GREEN hook blocked completion.", file=sys.stderr)
    print("Final verification must include passed command results.", file=sys.stderr)
    print(f"Reason: {validity_reason}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
