#!/usr/bin/env python3

import argparse
import difflib
import json
import subprocess
from datetime import datetime
from pathlib import Path

BRANCH = "AT_source_next_release"
SOURCE_DIR = "AndorsTrail/res/raw"
PROJECT_DIR = "altered/json"


def git(repo, *args, check=True):
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        capture_output=True,
        check=False,
    )
    if check and result.returncode != 0:
        print(
            f"python C:\\AT\\AndorsTrailRelease\\andors-trail\\AndorsTrail\\tools\\compare_json.py C:\\AT\\AndorsTrailRelease\\andors-trail\\AndorsTrail C:\\AT\\ATCS\\next_release\\ATCS\\next_Release > c:\\at\\temp\\cmp2.txt"
        )
        raise RuntimeError(
            result.stderr.decode("utf-8", errors="replace").strip()
        )
    return result


def file_at_commit(repo, commit, path):
    result = git(repo, "show", f"{commit}:{path}", check=False)
    if result.returncode != 0:
        return None
    return result.stdout.decode("utf-8-sig")


def load_elements(content, description):
    """Liest eine JSON-Liste als Dictionary, indiziert nach id."""
    if content is None:
        return None

    data = json.loads(content)
    if not isinstance(data, list):
        raise ValueError(f"{description}: keine JSON-Liste")

    elements = {}
    for element in data:
        if not isinstance(element, dict) or "id" not in element:
            raise ValueError(f"{description}: Element ohne id")

        element_id = element["id"]
        if not isinstance(element_id, str):
            raise ValueError(f"{description}: id ist kein String")
        if element_id in elements:
            print(
                f"WARNUNG: {description}: "
                f"doppelte ID {element_id} (erstes Vorkommen verwendet)"
            )
            continue

        elements[element_id] = element
    return elements


def project_element_creation(repo, path, wanted_ids):
    """Ermittelt den ersten Commit, in dem jede gesuchte ID innerhalb der Datei vorhanden war. Ergebnis: id -> (timestamp, commit_hash)    """
    result = git(
        repo, "log", "--follow",
        "--format=%H", "HEAD", "--", path
    )
    commits = result.stdout.decode().splitlines()

    # Älteste Version zuerst untersuchen.
    commits.reverse()

    found = {}
    missing = set(wanted_ids)

    for commit in commits:
        if not missing:
            break

        content = file_at_commit(repo, commit, path)
        if content is None:
            continue

        elements = load_elements(content, f"{path}@{commit[:12]}")
        new_ids = missing.intersection(elements)

        if new_ids:
            result = git(repo, "show", "-s", "--format=%ct", commit)
            timestamp = int(result.stdout.decode().strip())

            for element_id in new_ids:
                found[element_id] = (timestamp, commit)

            missing.difference_update(new_ids)

    return found


def branch_history(repo):
    result = git(
        repo, "log", "--first-parent",
        "--format=%ct %H", BRANCH
    )
    return [
        (int(line.split()[0]), line.split()[1])
        for line in result.stdout.decode().splitlines()
    ]


def commit_at_time(history, timestamp):
    for commit_time, commit_hash in history:
        if commit_time <= timestamp:
            return commit_hash
    return None


def format_element(element):
    return json.dumps(
        element,
        indent=2,
        ensure_ascii=False,
        sort_keys=True
    ).splitlines(keepends=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("Repo_AT", type=Path)
    parser.add_argument("Repo_Prj", type=Path)
    args = parser.parse_args()

    repo_at = args.Repo_AT.resolve()
    repo_prj = args.Repo_Prj.resolve()

    history = branch_history(repo_at)
    if not history:
        raise RuntimeError(f"Branch {BRANCH} ist leer")

    current_commit = history[0][1]

    files = sorted(
        (repo_prj / PROJECT_DIR).glob("*.json")
    )

    # Cache für historische AT-Dateien.
    at_cache = {}

    def at_elements(commit, path):
        key = (commit, path)
        if key not in at_cache:
            content = file_at_commit(repo_at, commit, path)
            at_cache[key] = load_elements(
                content, f"{path}@{commit[:12]}"
            )
        return at_cache[key]

    checked = 0
    changed = 0
    skipped = 0

    for file in files:
        project_path = f"{PROJECT_DIR}/{file.name}"
        source_path = f"{SOURCE_DIR}/{file.name}"

        current_project = load_elements(
            file.read_text(encoding="utf-8-sig"),
            str(file)
        )

        if not current_project:
            continue

        creation = project_element_creation(
            repo_prj, project_path, current_project.keys()
        )

        current_at = at_elements(current_commit, source_path)

        for element_id in current_project:
            checked += 1

            if element_id not in creation:
                print(
                    f"SKIP {file.name} / {element_id}: "
                    "kein erster Commit gefunden"
                )
                skipped += 1
                continue

            timestamp, project_commit = creation[element_id]
            old_commit = commit_at_time(history, timestamp)

            if old_commit is None:
                print(
                    f"SKIP {file.name} / {element_id}: "
                    "kein früherer AT-Commit"
                )
                skipped += 1
                continue

            old_at = at_elements(old_commit, source_path)

            if old_at is None or element_id not in old_at:
                print(
                    f"SKIP {file.name} / {element_id}: "
                    "Element damals in AT nicht vorhanden"
                )
                skipped += 1
                continue

            if current_at is None or element_id not in current_at:
                print(
                    f"SKIP {file.name} / {element_id}: "
                    "Element aktuell in AT nicht vorhanden"
                )
                skipped += 1
                continue

            old_element = old_at[element_id]
            new_element = current_at[element_id]

            # JSON-Strukturvergleich statt Textvergleich.
            if old_element == new_element:
                continue

            changed += 1
            date = datetime.fromtimestamp(timestamp).astimezone()

            print(f"\n{'=' * 70}")
            print(f"Datei:     {file.name}")
            print(f"ID:        {element_id}")
            print(f"Erstellt:  {date:%Y-%m-%d %H:%M:%S %z}")
            print(f"Projekt:   {project_commit[:12]}")
            print(f"AT damals: {old_commit[:12]}")
            print(f"AT jetzt:  {current_commit[:12]}")
            print("-" * 70)

            diff = difflib.unified_diff(
                format_element(old_element),
                format_element(new_element),
                fromfile="damals",
                tofile="aktuell"
            )
            print("".join(diff), end="")

    print(f"\n{'=' * 70}")
    print(f"JSON-Dateien:        {len(files)}")
    print(f"Geprüfte Elemente:   {checked}")
    print(f"Geänderte Elemente:  {changed}")
    print(f"Übersprungene:       {skipped}")


if __name__ == "__main__":
    main()
