#!/usr/bin/env python3
"""
Convert integer digit runs inside JSON string values to <num>...</num> tags.

Usage:
  ./convert_numbers_to_num_tags.py [--path PATH] [--min-digits N] [--dry-run] [--inplace] [--backup]

Examples:
  # dry run, default matches any digit sequence (min-digits=1) in res/raw
  ./convert_numbers_to_num_tags.py --path /home/sjhudson/AndroidStudioProjects/andors-trail/AndorsTrail/res/raw --dry-run

  # only wrap sequences of 4 or more digits (years, large numbers)
  ./convert_numbers_to_num_tags.py --min-digits 4 --inplace

Notes:
- Only string values in JSON are processed. Numeric JSON values (unquoted numbers) are left alone.
- By default the script writes a .bak copy for each modified file when run with --inplace.
- The script avoids double-wrapping numbers already inside <num>...</num> sections.

This script is intentionally conservative and provides a dry-run mode. Review diffs before committing changes.
"""

import argparse
import json
import os
import re
import sys
from typing import Any, Tuple


def parse_args():
    p = argparse.ArgumentParser(description="Wrap digit runs inside JSON string values with <num> tags")
    p.add_argument("--path", default="res/raw",
                   help="Path to the raw JSON directory (default: res/raw relative to cwd)")
    p.add_argument("--min-digits", type=int, default=1,
                   help="Minimum number of consecutive digits to match (default: 1 => match all numbers)")
    p.add_argument("--dry-run", action="store_true", help="Do not modify files; print summary only")
    p.add_argument("--inplace", action="store_true", help="Modify files in-place (creates .bak backups unless --no-backup)")
    p.add_argument("--no-backup", dest="backup", action="store_false", help="Do not create .bak backups when using --inplace")
    p.add_argument("--encoding", default="utf-8", help="File encoding (default utf-8)")
    p.add_argument("--verbose", "-v", action="store_true")
    return p.parse_args()


def compile_pattern(min_digits: int) -> re.Pattern:
    # match a contiguous run of digits with word boundaries on both sides
    # we use lookarounds to avoid capturing digits that are part of larger tokens
    pat = r"(?<!\d)(\d{%d,})(?!\d)" % (min_digits,)
    return re.compile(pat)


def should_skip_match(s: str, start: int, end: int) -> bool:
    """Return True if the match [start:end] sits inside an existing <num>...</num> tag in s.
    This is a conservative check: if there's a '<num' before the match and a matching '</num>' after it,
    we assume the match is inside a tag and should be left alone.
    """
    before = s.rfind('<num', 0, start)
    if before == -1:
        return False
    after = s.find('</num>', end)
    return after != -1


def replace_in_string(s: str, pattern: re.Pattern) -> Tuple[str, int]:
    """Return (new_string, replacements) where numeric matches are wrapped with <num> tags.

    The function avoids wrapping numbers that are already inside <num>...</num>.
    """
    if not s:
        return s, 0

    replacements = 0

    def repl(match: re.Match):
        nonlocal replacements
        st, ed = match.start(1), match.end(1)
        # if already inside <num>...</num>, skip
        if should_skip_match(s, st, ed):
            return match.group(1)
        replacements += 1
        return '<num>' + match.group(1) + '</num>'

    new = pattern.sub(repl, s)
    return new, replacements


def process_value(v: Any, pattern: re.Pattern, parent_key: str = None) -> Tuple[Any, int]:
    """Recursively process JSON value v. Returns (new_value, replacements_count).
    Only string values whose key is 'message' or 'text' are altered; other types are traversed recursively.
    parent_key is the containing dictionary key for v (if any).
    """
    if isinstance(v, str):
        # Only process strings that are values of keys named 'message' or 'text'
        if parent_key in ("message", "text"):
            return replace_in_string(v, pattern)
        else:
            return v, 0
    elif isinstance(v, list):
        total = 0
        new_list = []
        for elem in v:
            # preserve parent_key for elements in lists
            new_elem, cnt = process_value(elem, pattern, parent_key)
            total += cnt
            new_list.append(new_elem)
        return new_list, total
    elif isinstance(v, dict):
        total = 0
        new_obj = {}
        for k, val in v.items():
            # pass the current key as parent_key for the value
            new_val, cnt = process_value(val, pattern, k)
            total += cnt
            new_obj[k] = new_val
        return new_obj, total
    else:
        return v, 0


def process_file(path: str, pattern: re.Pattern, encoding: str, inplace: bool, backup: bool, dry_run: bool, verbose: bool) -> Tuple[int, bool]:
    """Process one JSON file. Returns (replacements, changed_bool)"""
    try:
        with open(path, 'r', encoding=encoding) as f:
            data = json.load(f)
    except Exception as e:
        print(f"Skipping {path}: failed to parse JSON: {e}", file=sys.stderr)
        return 0, False

    new_data, replacements = process_value(data, pattern)
    if replacements == 0:
        if verbose:
            print(f"No changes in {path}")
        return 0, False

    if dry_run:
        print(f"DRY RUN: {path}: would wrap {replacements} number(s)")
        return replacements, True

    # write changes
    if inplace:
        if backup:
            bak = path + '.bak'
            try:
                if os.path.exists(bak):
                    os.remove(bak)
                os.rename(path, bak)
            except Exception as e:
                print(f"Warning: could not create backup for {path}: {e}", file=sys.stderr)
        try:
            with open(path, 'w', encoding=encoding) as f:
                json.dump(new_data, f, ensure_ascii=False, indent=4, sort_keys=False)
                f.write('\n')
            print(f"Updated {path}: wrapped {replacements} number(s)")
            return replacements, True
        except Exception as e:
            print(f"Failed to write {path}: {e}", file=sys.stderr)
            return 0, False
    else:
        # Not inplace: write to a sibling file with suffix .num.json
        outpath = path + '.num.json'
        try:
            with open(outpath, 'w', encoding=encoding) as f:
                json.dump(new_data, f, ensure_ascii=False, indent=4, sort_keys=False)
                f.write('\n')
            print(f"Wrote {outpath}: wrapped {replacements} number(s)")
            return replacements, True
        except Exception as e:
            print(f"Failed to write {outpath}: {e}", file=sys.stderr)
            return 0, False


def main():
    args = parse_args()
    base = args.path
    if not os.path.isabs(base):
        base = os.path.join(os.getcwd(), base)

    if not os.path.isdir(base):
        print(f"Path not found or not a directory: {base}", file=sys.stderr)
        sys.exit(1)

    pattern = compile_pattern(args.min_digits)

    total_files = 0
    total_replacements = 0
    changed_files = 0

    for root, dirs, files in os.walk(base):
        for fn in sorted(files):
            if not fn.lower().endswith('.json'):
                continue
            path = os.path.join(root, fn)
            total_files += 1
            replacements, changed = process_file(path, pattern, args.encoding, args.inplace, args.backup, args.dry_run, args.verbose)
            total_replacements += replacements
            if changed:
                changed_files += 1

    print("\nSummary:")
    print(f"  scanned files: {total_files}")
    print(f"  files changed: {changed_files}")
    print(f"  total numbers wrapped: {total_replacements}")


if __name__ == '__main__':
    main()

