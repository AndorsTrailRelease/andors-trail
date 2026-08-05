#!/usr/bin/env python3
"""Wrap bare numbers in conversation text with curly braces.

This updates `message` and `text` fields in `conversationlist_*.json` files so
numeric literals with 4+ integer digits become brace-wrapped placeholders like
`{1000}` or `{1234.5}`.
Existing brace-wrapped numbers are left unchanged.
"""

from __future__ import annotations

import argparse
import difflib
import json
import os
from pathlib import Path
from typing import Any, Iterable

from brace_number_common import (
    brace_numbers,
    default_exclude_ids_config_path,
    is_excluded_id,
    load_exclude_ids_file,
    normalize_exclude_ids,
)
TARGET_KEYS = {"message", "text"}


def transform_value(value: Any, key: str | None = None, exclude_ids: set[str] | None = None) -> tuple[Any, int]:
    if isinstance(value, dict):
        source_id = value.get("id") if isinstance(value.get("id"), str) else None
        if source_id and exclude_ids and is_excluded_id(source_id, exclude_ids):
            return value, 0

        changed = 0
        result = {}
        for key, child in value.items():
            new_child, child_changes = transform_value(child, key, exclude_ids)
            result[key] = new_child
            changed += child_changes
        return result, changed

    if isinstance(value, list):
        changed = 0
        result = []
        for item in value:
            new_item, child_changes = transform_value(item, key, exclude_ids)
            result.append(new_item)
            changed += child_changes
        return result, changed

    if isinstance(value, str):
        if key in TARGET_KEYS:
            return brace_numbers(value)
        return value, 0

    return value, 0


def print_changed_lines(path: Path, original: str, updated: str) -> None:
    original_lines = original.splitlines()
    updated_lines = updated.splitlines()
    matcher = difflib.SequenceMatcher(a=original_lines, b=updated_lines)

    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            continue

        old_block = original_lines[i1:i2]
        new_block = updated_lines[j1:j2]
        max_len = max(len(old_block), len(new_block))

        for offset in range(max_len):
            old_line = old_block[offset] if offset < len(old_block) else ""
            new_line = new_block[offset] if offset < len(new_block) else ""
            old_no = i1 + offset + 1 if offset < len(old_block) else ""
            new_no = j1 + offset + 1 if offset < len(new_block) else ""
            print(f"{path}: line {old_no} -> {new_no}")
            print(f"  before: {old_line}")
            print(f"  after : {new_line}")


def discover_files(paths: Iterable[Path]) -> list[Path]:
    files: list[Path] = []
    seen: set[Path] = set()

    for base in paths:
        if base.is_file():
            if base.name.startswith("conversationlist_") and base.suffix == ".json":
                resolved = base.resolve()
                if resolved not in seen:
                    files.append(base)
                    seen.add(resolved)
            continue

        if not base.is_dir():
            continue

        for match in base.rglob("conversationlist_*.json"):
            resolved = match.resolve()
            if resolved in seen:
                continue
            seen.add(resolved)
            files.append(match)

    return sorted(files)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Wrap bare numbers in conversationlist message/text fields with curly braces."
    )
    parser.add_argument(
        "paths",
        nargs="+",
        help="Files or directories to scan.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Report changes without rewriting files.",
    )
    parser.add_argument(
        "--show-changes",
        action="store_true",
        help="Print each changed line before and after.",
    )
    parser.add_argument(
        "--exclude-id",
        action="append",
        default=[],
        help="Source IDs to leave unchanged. Can be repeated.",
    )
    parser.add_argument(
        "--exclude-config",
        default=str(default_exclude_ids_config_path()),
        help="Path to a text file with one excluded ID per line.",
    )
    args = parser.parse_args()
    exclude_ids = normalize_exclude_ids(args.exclude_id) | load_exclude_ids_file(args.exclude_config)

    files = discover_files(Path(p) for p in args.paths)
    total_changes = 0

    for path in files:
        original = path.read_text(encoding="utf-8")
        data = json.loads(original)
        transformed, changes = transform_value(data, exclude_ids=exclude_ids)
        if changes == 0:
            continue

        updated = json.dumps(transformed, ensure_ascii=False, indent=4, separators=(",", ":")) + "\n"
        if args.show_changes:
            print_changed_lines(path, original, updated)
        if not args.dry_run:
            path.write_text(updated, encoding="utf-8")
        print(f"{path}: {changes} replacement(s)")
        total_changes += changes

    if total_changes == 0:
        print("No changes.")
    else:
        print(f"{total_changes} total replacement(s)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
