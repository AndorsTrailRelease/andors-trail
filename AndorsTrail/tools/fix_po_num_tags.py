#!/usr/bin/env python3
"""
Auto-insert <num> tags into .po msgstr entries to match msgid which contains <num> tags.
This tool is conservative and only auto-fixes entries where the numeric literal in the
msgid appears unchanged (ASCII digits) in the msgstr. It does not alter msgid or msgctxt,
and it can optionally clear the 'fuzzy' flag if the change is unambiguous.

Usage:
  python3 tools/fix_po_num_tags.py --po-dir assets/translation --dry-run
  python3 tools/fix_po_num_tags.py --po-dir assets/translation --apply

Notes:
- Requires 'polib' (pip install polib).
- Always run with --dry-run first and review diffs before applying.
- The script will create a .bak copy of each .po it modifies.
- Plural forms are skipped (not auto-fixed) to avoid pluralization mistakes.

Behavior:
- For each entry where msgid contains one or more <num...>NNN</num> tags:
  - If the same ASCII digit sequence NNN appears verbatim in msgstr, the first occurrence
    is replaced with the tagged form. (We assume translators left digits as ASCII digits.)
  - If all numeric tags in msgid were matched and replaced, the entry's 'fuzzy' flag
    is removed (optional, default true).
  - The script adds a translator comment indicating an automated insertion.

This approach is safe for the common case where translators kept the digits unchanged and
only the English source wrapped them with <num> tags. For localized digits (non-ASCII)
or reworded numbers the script will skip the entry.
"""

from __future__ import annotations
import argparse
import os
import re
import shutil
import sys
from typing import List

try:
    import polib
except Exception:
    print("polib is required. Install with: pip install polib", file=sys.stderr)
    raise

NUM_TAG_RE = re.compile(r"<num(?:\s+fmt=(?:'|\")?([^'\">]+)(?:'|\")?)?>(-?\d+)</num>", re.IGNORECASE)
ASCII_DIGITS_RE = re.compile(r"[0-9-]+")


def normalize_ascii_digits(s: str) -> str:
    return ''.join(ch for ch in s if '0' <= ch <= '9' or ch == '-')


def try_fix_entry(entry: polib.POEntry, remove_fuzzy: bool = True) -> bool:
    """Attempt to update entry.msgstr to include tags from entry.msgid.
    Returns True if change was applied, False otherwise.
    Does not touch entry.msgid.
    """
    if not entry.msgid or not entry.msgstr:
        return False

    if entry.msgstr_plural:
        # skip plural entries for safety
        return False

    tags = list(NUM_TAG_RE.finditer(entry.msgid))
    if not tags:
        return False

    new_msgstr = entry.msgstr
    # We'll try to replace each numeric literal in order with the tagged form.
    # For safety, we require that the plain ASCII digits appear in msgstr.
    for t in tags:
        num = t.group(2)
        fmt = t.group(1)
        # find literal ASCII digits in msgstr
        m = ASCII_DIGITS_RE.search(new_msgstr)
        if not m:
            # no ASCII digits to match, skip
            return False
        found = m.group(0)
        if found != num:
            # found digits differ from the msgid digits -> unsafe
            return False
        # build replacement preserving fmt style (use single quotes)
        if fmt:
            repl = f"<num fmt='{fmt}'>{num}</num>"
        else:
            repl = f"<num>{num}</num>"
        # replace only the first occurrence
        new_msgstr = new_msgstr[:m.start()] + repl + new_msgstr[m.end():]

    if new_msgstr == entry.msgstr:
        return False

    # Apply changes
    entry.msgstr = new_msgstr
    note = 'Automated: inserted <num> tags to match updated source; please verify.'
    if entry.tcomment:
        entry.tcomment = entry.tcomment + '\n' + note
    else:
        entry.tcomment = note

    if remove_fuzzy and 'fuzzy' in entry.flags:
        try:
            entry.flags.remove('fuzzy')
        except ValueError:
            pass
    return True


def process_po_file(path: str, dry_run: bool = True, remove_fuzzy: bool = True, verbose: bool = False) -> int:
    po = polib.pofile(path)
    changed = 0
    for entry in po:
        try:
            ok = try_fix_entry(entry, remove_fuzzy=remove_fuzzy)
        except Exception as e:
            if verbose:
                print(f"Skipping entry due to exception: {e}")
            ok = False
        if ok:
            changed += 1
            if verbose:
                print(f"Will update entry in {path}: msgid={entry.msgid[:60]!r}")
    if changed and not dry_run:
        bak = path + '.bak'
        shutil.copy2(path, bak)
        po.save(path)
        print(f"Saved {path} (backup at {bak})")
    return changed


def main(argv: List[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description='Auto-fix <num> tags in .po msgstr entries')
    ap.add_argument('--po-dir', default='assets/translation', help='Directory with .po files')
    ap.add_argument('--apply', action='store_true', help='Apply changes (otherwise dry-run)')
    ap.add_argument('--no-remove-fuzzy', action='store_true', help="Don't remove 'fuzzy' flags automatically")
    ap.add_argument('--verbose', '-v', action='store_true')
    args = ap.parse_args(argv)

    po_dir = args.po_dir
    if not os.path.isdir(po_dir):
        print(f"Directory not found: {po_dir}", file=sys.stderr)
        return 2

    total_changed = 0
    for fn in sorted(os.listdir(po_dir)):
        if not fn.endswith('.po'):
            continue
        path = os.path.join(po_dir, fn)
        try:
            changed = process_po_file(path, dry_run=not args.apply, remove_fuzzy=not args.no_remove_fuzzy, verbose=args.verbose)
            if changed:
                print(f"{fn}: {changed} entries updated {'(applied)' if args.apply else '(dry-run)'}")
            total_changed += changed
        except Exception as e:
            print(f"Error processing {path}: {e}", file=sys.stderr)

    print(f"Total entries updated: {total_changed}")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

