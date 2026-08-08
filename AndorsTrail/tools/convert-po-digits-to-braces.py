#!/usr/bin/env python3
"""Wrap bare numbers in gettext .po/.pot files with curly braces.

This updates msgid/msgstr text in .po files so numeric literals with 4+ digits
in the integer part become brace-wrapped placeholders like {1000} or {1234.5}.
Existing brace-wrapped numbers are left unchanged.
"""

from __future__ import annotations

import argparse
import difflib
import os
import re
from pathlib import Path
from typing import Iterable

from brace_number_common import (
    brace_numbers,
    default_exclude_ids_config_path,
    load_exclude_ids_file,
    normalize_exclude_ids,
)
FIELD_RE = re.compile(
    r'^(?P<indent>\s*)(?P<label>msgid_plural|msgid|msgstr(?:\[\d+\])?)\s+"(?P<content>(?:[^"\\]|\\.)*)"\s*$'
)
CONT_RE = re.compile(r'^(?P<indent>\s*)"(?P<content>(?:[^"\\]|\\.)*)"\s*$')
SOURCE_REF_RE = re.compile(r'^\s*#:\s*(?P<refs>.+?)\s*$')


def is_target_label(label: str) -> bool:
    return label == "msgid" or label == "msgid_plural" or label.startswith("msgstr[") or label == "msgstr"


def is_header_entry(lines: list[str]) -> bool:
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        m = FIELD_RE.match(stripped)
        if m and m.group("label") == "msgid":
            return m.group("content") == ""
        return False
    return False


def entry_source_ids(lines: list[str]) -> set[str]:
    refs: set[str] = set()
    for line in lines:
        m = SOURCE_REF_RE.match(line.rstrip("\r\n"))
        if not m:
            continue
        for ref in m.group("refs").split():
            parts = [part for part in ref.split(":") if part]
            refs.update(parts)
    return refs


def transform_entry(lines: list[str], exclude_ids: set[str]) -> tuple[list[str], int]:
    if not lines or is_header_entry(lines):
        return lines, 0
    if entry_source_ids(lines) & exclude_ids:
        return lines, 0

    out: list[str] = []
    current_target = False
    changes = 0

    for raw in lines:
        newline = ""
        stripped = raw
        if raw.endswith("\r\n"):
            stripped = raw[:-2]
            newline = "\r\n"
        elif raw.endswith("\n"):
            stripped = raw[:-1]
            newline = "\n"

        m = FIELD_RE.match(stripped)
        if m:
            label = m.group("label")
            current_target = is_target_label(label)
            content = m.group("content")
            if current_target:
                content, count = brace_numbers(content)
                changes += count
            out.append(f'{m.group("indent")}{label} "{content}"{newline}')
            continue

        m = CONT_RE.match(stripped)
        if m and current_target:
            content, count = brace_numbers(m.group("content"))
            changes += count
            out.append(f'{m.group("indent")}"{content}"{newline}')
            continue

        current_target = False
        out.append(raw)

    return out, changes


def count_changed_lines(original: str, updated: str) -> int:
    original_lines = original.splitlines()
    updated_lines = updated.splitlines()
    matcher = difflib.SequenceMatcher(a=original_lines, b=updated_lines)
    count = 0

    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag != "equal":
            count += max(i2 - i1, j2 - j1)

    return count


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
            if base.suffix in {".po", ".pot"}:
                resolved = base.resolve()
                if resolved not in seen:
                    files.append(base)
                    seen.add(resolved)
            continue

        if not base.is_dir():
            continue

        for match in base.rglob("*.po"):
            resolved = match.resolve()
            if resolved in seen:
                continue
            seen.add(resolved)
            files.append(match)

        for match in base.rglob("*.pot"):
            resolved = match.resolve()
            if resolved in seen:
                continue
            seen.add(resolved)
            files.append(match)

    return sorted(files)


def process_file(path: Path, dry_run: bool, show_changes: bool, exclude_ids: set[str]) -> tuple[int, int]:
    original = path.read_text(encoding="utf-8")
    lines = original.splitlines(keepends=True)

    out: list[str] = []
    entry: list[str] = []
    total_replacements = 0
    total_changed_lines = 0

    def flush_entry() -> None:
        nonlocal entry, total_replacements, total_changed_lines
        if not entry:
            return
        transformed, replacements = transform_entry(entry, exclude_ids)
        updated_entry = "".join(transformed)
        original_entry = "".join(entry)
        if replacements:
            total_replacements += replacements
            total_changed_lines += count_changed_lines(original_entry, updated_entry)
            if show_changes:
                print_changed_lines(path, original_entry, updated_entry)
        out.extend(transformed)
        entry = []

    for line in lines:
        if line.strip() == "":
            flush_entry()
            out.append(line)
        else:
            entry.append(line)

    flush_entry()

    updated = "".join(out)
    if total_replacements and not dry_run:
        path.write_text(updated, encoding="utf-8")

    return total_replacements, total_changed_lines


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Wrap bare numbers in .po/.pot msgid/msgstr fields with curly braces."
    )
    parser.add_argument("paths", nargs="+", help="Files or directories to scan.")
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
    total_replacements = 0
    total_converted_lines = 0

    for path in files:
        replacements, converted_lines = process_file(path, args.dry_run, args.show_changes, exclude_ids)
        if replacements == 0:
            continue
        print(f"{path}: {replacements} replacement(s)")
        total_replacements += replacements
        total_converted_lines += converted_lines

    if total_replacements == 0:
        print("No changes.")
    else:
        print(f"Total converted lines: {total_converted_lines}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
