"""python C:\AT\AndorsTrailRelease\andors-trail\AndorsTrail\tools\compare_maps.py C:\AT\AndorsTrailRelease\andors-trail\AndorsTrail C:\AT\ATCS\next_release\ATCS\next_Release > c:\at\temp\cmp.txt"""

#!/usr/bin/env python3
import argparse
import difflib
import subprocess
from datetime import datetime
from pathlib import Path

BRANCH = "AT_source_next_release"
SOURCE_DIR = "AndorsTrail/res/xml"
PROJECT_DIR = "altered/maps"


def git(repo, *args, check=True):
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        capture_output=True,
        check=False,
    )
    if check and result.returncode != 0:
        raise RuntimeError(
            result.stderr.decode("utf-8", errors="replace").strip()
        )
    return result


def creation_timestamp(repo, path):
    """Zeitpunkt der ersten Aufnahme einer Datei in Git."""
    result = git(
        repo, "log", "--diff-filter=A",
        "--format=%ct", "--", path
    )
    timestamps = result.stdout.decode().splitlines()
    if not timestamps:
        return None
    return int(timestamps[-1])


def branch_history(repo):
    """First-Parent-Commits, neuester zuerst."""
    result = git(
        repo, "log", "--first-parent",
        "--format=%ct %H", BRANCH
    )
    return [
        (int(line.split()[0]), line.split()[1])
        for line in result.stdout.decode().splitlines()
    ]


def commit_at_time(history, timestamp):
    """Letzter First-Parent-Commit vor dem Zeitpunkt."""
    for commit_time, commit_hash in history:
        if commit_time <= timestamp:
            return commit_hash
    return None


def file_at_commit(repo, commit, path):
    """Datei direkt aus Git lesen, ohne Checkout."""
    result = git(
        repo, "show", f"{commit}:{path}", check=False
    )
    if result.returncode != 0:
        return None
    return result.stdout.decode("utf-8", errors="replace")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("Repo_AT", type=Path)
    parser.add_argument("Repo_Prj", type=Path)
    args = parser.parse_args()

    repo_at = args.Repo_AT.resolve()
    repo_prj = args.Repo_Prj.resolve()

    files = sorted((repo_prj / PROJECT_DIR).glob("*"))
    files = [f for f in files if f.is_file()]

    history = branch_history(repo_at)
    if not history:
        raise RuntimeError(f"Branch {BRANCH} ist leer")

    current_commit = history[0][1]
    changed = 0

    for file in files:
        project_path = f"{PROJECT_DIR}/{file.name}"
        source_path = f"{SOURCE_DIR}/{file.name}"

        timestamp = creation_timestamp(repo_prj, project_path)
        if timestamp is None:
            print(f"SKIP {file.name}: keine Git-Erstellung gefunden")
            continue

        old_commit = commit_at_time(history, timestamp)
        if old_commit is None:
            print(f"SKIP {file.name}: kein früherer AT-Commit")
            continue

        old_text = file_at_commit(repo_at, old_commit, source_path)
        new_text = file_at_commit(repo_at, current_commit, source_path)

        if old_text is None or new_text is None:
            print(f"SKIP {file.name}: Datei in AT nicht vorhanden")
            continue

        if old_text == new_text:
            continue

        changed += 1
        date = datetime.fromtimestamp(timestamp).astimezone()

        print(f"\n{'=' * 70}")
        print(f"Datei:   {file.name}")
        print(f"Erstellt: {date:%Y-%m-%d %H:%M:%S %z}")
        print(f"Von:     {old_commit[:12]}")
        print(f"Bis:     {current_commit[:12]}")
        print("-" * 70)

        diff = difflib.unified_diff(
            old_text.splitlines(keepends=True),
            new_text.splitlines(keepends=True),
            fromfile=f"{file.name} (damals)",
            tofile=f"{file.name} (aktuell)",
        )
        print("".join(diff), end="")

    print(f"\nFertig. {len(files)} Dateien geprüft, "
          f"{changed} mit Änderungen.")


if __name__ == "__main__":
    main()
