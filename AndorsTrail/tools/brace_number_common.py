from __future__ import annotations

import re
from pathlib import Path
from typing import Iterable


NUMBER_TOKEN_RE = re.compile(
    r"(\{?\s*-?\d{4,}(?:\.\d+)?\s*\}|(?<![.\w])-?\d{4,}(?:\.\d+)?(?![.\w]))"
)


def brace_numbers(text: str) -> tuple[str, int]:
    replacements = 0

    def repl(match: re.Match[str]) -> str:
        nonlocal replacements
        token = match.group(1)
        if token.startswith("{"):
            return token
        replacements += 1
        return "{" + token + "}"

    return NUMBER_TOKEN_RE.sub(repl, text), replacements


def normalize_exclude_ids(values: Iterable[str] | None) -> set[str]:
    if not values:
        return set()
    return {value.strip() for value in values if value and value.strip()}


def load_exclude_ids_file(path: str | Path | None) -> set[str]:
    if not path:
        return set()

    text = Path(path).read_text(encoding="utf-8")
    exclude_ids: set[str] = set()
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        exclude_ids.add(line)
    return exclude_ids


def default_exclude_ids_config_path() -> Path:
    return Path(__file__).with_name("exclude-ids.txt")


def is_excluded_id(source_id: str | None, exclude_ids: set[str]) -> bool:
    if not source_id:
        return False
    return source_id in exclude_ids
