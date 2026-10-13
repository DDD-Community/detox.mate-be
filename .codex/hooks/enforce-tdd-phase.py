#!/usr/bin/env python3
"""Block product-code edits until the Detoxmate TDD RED record exists."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


RED_RECORD = Path("_workspace/03_test-designer_red-record.md")
PRODUCT_PREFIX = "src/main/"


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


def read_hook_payload() -> dict[str, Any]:
    raw = sys.stdin.read()
    if not raw.strip():
        raw = os.environ.get("CODEX_HOOK_INPUT", "")
    if not raw.strip():
        return {}

    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        print(f"TDD hook warning: could not parse hook input as JSON: {exc}", file=sys.stderr)
        return {}

    if isinstance(payload, dict):
        return payload
    return {}


def normalize_path(value: str, root: Path) -> str | None:
    value = value.strip().strip('"').strip("'")
    if not value:
        return None

    if value.startswith("a/") or value.startswith("b/"):
        value = value[2:]
    if value.startswith("./"):
        value = value[2:]

    path = Path(value)
    if path.is_absolute():
        try:
            value = path.resolve().relative_to(root).as_posix()
        except ValueError:
            return None
    else:
        value = path.as_posix()

    return value


def paths_from_patch(patch: str, root: Path) -> set[str]:
    paths: set[str] = set()
    patterns = (
        re.compile(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$"),
        re.compile(r"^\*\*\* Move to: (.+)$"),
        re.compile(r"^diff --git a/(.+?) b/(.+)$"),
        re.compile(r"^(?:---|\+\+\+) [ab]/(.+)$"),
    )

    for line in patch.splitlines():
        for pattern in patterns:
            match = pattern.match(line)
            if not match:
                continue
            for group in match.groups():
                path = normalize_path(group, root)
                if path and path != "/dev/null":
                    paths.add(path)
    return paths


def collect_paths(value: Any, root: Path) -> set[str]:
    paths: set[str] = set()

    if isinstance(value, dict):
        for key, child in value.items():
            key_name = str(key).lower()
            if isinstance(child, str):
                if key_name in {"file_path", "filepath", "path", "target_file", "filename"}:
                    path = normalize_path(child, root)
                    if path:
                        paths.add(path)
                if key_name in {"patch", "diff", "content"}:
                    paths.update(paths_from_patch(child, root))
            else:
                paths.update(collect_paths(child, root))
    elif isinstance(value, list):
        for child in value:
            paths.update(collect_paths(child, root))
    elif isinstance(value, str):
        paths.update(paths_from_patch(value, root))

    return paths


def red_record_is_valid(root: Path) -> tuple[bool, str]:
    path = root / RED_RECORD
    if not path.exists():
        return False, f"missing {RED_RECORD.as_posix()}"

    text = path.read_text(encoding="utf-8", errors="replace").lower()
    has_test_command = "gradlew" in text and "test" in text
    has_failed_status = any(
        marker in text
        for marker in (
            "status: failed",
            "status: fail",
            "failed",
            "failing",
            "failure",
        )
    )
    has_requirement_reason = any(
        marker in text
        for marker in (
            "requirements red",
            "requirement red",
            "requirement not implemented",
            "missing behavior",
            "why this is requirements red",
        )
    )

    missing: list[str] = []
    if not has_test_command:
        missing.append("gradle test command")
    if not has_failed_status:
        missing.append("failed RED status")
    if not has_requirement_reason:
        missing.append("requirement-RED reason")

    if missing:
        return False, f"{RED_RECORD.as_posix()} is missing: {', '.join(missing)}"
    return True, "valid"


def main() -> int:
    root = repo_root()
    payload = read_hook_payload()
    target_paths = collect_paths(payload, root)

    product_paths = sorted(path for path in target_paths if path.startswith(PRODUCT_PREFIX))
    if not product_paths:
        return 0

    is_valid, reason = red_record_is_valid(root)
    if is_valid:
        return 0

    print("Detoxmate TDD hook blocked a product-code edit.", file=sys.stderr)
    print("src/main/** may be edited only after a valid RED record exists.", file=sys.stderr)
    print(f"Reason: {reason}", file=sys.stderr)
    print("Blocked paths:", file=sys.stderr)
    for path in product_paths:
        print(f"- {path}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
