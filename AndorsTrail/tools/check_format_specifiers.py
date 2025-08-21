import re
import os
import argparse
from xml.etree import ElementTree as ET

def get_string_value_and_specifiers(filepath, key_name):
    """
    Parses an XML strings file to find a specific key and its format specifiers.

    Args:
        filepath (str): Path to the strings.xml file.
        key_name (str): The name of the string resource to find.

    Returns:
        tuple: (string_value, set_of_specifiers) or (None, None) if key not found.
               Specifiers are returned as a set, e.g., {"%1$s", "%2$d"}.
    """
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        for string_tag in root.findall('string'):
            if string_tag.get('name') == key_name:
                value = string_tag.text if string_tag.text else ""
                # Regex to find format specifiers like %s, %d, %1$s, %2$d, etc.
                # It handles optional positional arguments (e.g., 1$) and type characters.
                specifiers = set(re.findall(r'%(?:(?:\d+\$)?(?:[sdfeoxXgGaAbhHc]|(?:\.\d[fd])))', value))
                return value, specifiers
    except ET.ParseError:
        print(f"Warning: Could not parse XML file: {filepath}")
    except FileNotFoundError:
        print(f"Warning: File not found: {filepath}")
    return None, None

def find_res_directories(project_root):
    """
    Finds all 'res' directories within a project, typically in module roots.
    """
    res_dirs = []
    for root_dir, dirs, _ in os.walk(project_root):
        if 'res' in dirs:
            res_dirs.append(os.path.join(root_dir, 'res'))
    if not res_dirs and os.path.basename(project_root) == 'res': # If project_root itself is a res dir
        res_dirs.append(project_root)
    return res_dirs


def find_strings_files(res_dir_path, base_filename="strings.xml"):
    """
    Finds all strings.xml files (or variants like strings-es.xml)
    within a given 'res' directory.
    """
    strings_files = {} # lang_code -> filepath
    for dirpath, _, filenames in os.walk(res_dir_path):
        if "values" in os.path.basename(dirpath).lower(): # e.g., values, values-es, values-en-rGB
            for filename in filenames:
                if filename.startswith(os.path.splitext(base_filename)[0]) and filename.endswith(".xml"):
                    full_path = os.path.join(dirpath, filename)
                    # Determine language code from directory name (e.g., "values-es" -> "es")
                    # or default if it's just "values"
                    dir_name_parts = os.path.basename(dirpath).split('-')
                    lang_code = "default" # For the base "values" folder
                    if len(dir_name_parts) > 1:
                        lang_code = "-".join(dir_name_parts[1:]) # Handles values-en-rUS correctly
                    strings_files[lang_code] = full_path
    return strings_files

def find_non_escaped_percent(value):
    """
    Finds non-escaped % characters in a string (not part of %% or a valid format specifier).
    Returns a list of indices where such % occur.
    """
    # Find all % positions
    percent_indices = [m.start() for m in re.finditer(r'%', value)]
    # Find all valid format specifiers and %% positions
    valid_specifier_pattern = r'%(?:%|(?:\d+\$)?(?:[sdfeoxXgGaAbhHc]|(?:\.\d[fd])))'
    valid_matches = [m.span() for m in re.finditer(valid_specifier_pattern, value)]
    # Mark all indices covered by valid specifiers or %%
    covered_indices = set()
    for start, end in valid_matches:
        covered_indices.update(range(start, end))
    # Only report % indices not covered by valid specifiers or %%
    return [idx for idx in percent_indices if idx not in covered_indices]

def get_all_keys(filepath):
    """
    Returns a list of all string resource keys in the given XML file.
    """
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        return [string_tag.get('name') for string_tag in root.findall('string') if string_tag.get('name')]
    except Exception:
        return []

def find_used_keys_in_java(project_root):
    """
    Scans all .java files under project_root for usages of string resource keys.
    Returns a set of keys found.
    """
    key_pattern = re.compile(r'R\.string\.([a-zA-Z0-9_]+)')
    used_keys = set()
    for root, _, files in os.walk(project_root):
        for fname in files:
            if fname.endswith('.java'):
                try:
                    with open(os.path.join(root, fname), encoding='utf-8') as f:
                        content = f.read()
                        used_keys.update(key_pattern.findall(content))
                except Exception:
                    pass
    return used_keys

def main():
    parser = argparse.ArgumentParser(
        description="Check format specifier consistency across language files for a given string key."
    )
    parser.add_argument(
        "project_root",
        help="Path to the Android project's root directory (or a specific module's root directory)."
    )
    parser.add_argument(
        "res_root",
        help="Path to the some 'res' directory."
    )
    parser.add_argument(
        "key_name",
        nargs="?",
        default=None,
        help="The name of the string resource to check (e.g., 'skill_longdescription_evasion'). If omitted, checks all keys in the base language file."
    )
    parser.add_argument(
        "--base_lang",
        default="default",
        help="The language code for the base/reference strings file (e.g., 'en', 'default' for values/strings.xml). Default is 'default'."
    )
    parser.add_argument(
        "--strings_filename",
        default="strings.xml",
        help="The base name of your strings files (default: strings.xml)."
    )

    args = parser.parse_args()

    print(f"Using base language: '{args.base_lang}' from file '{args.strings_filename}'")
    print(f"Project root path: {args.project_root}\n")
    print(f"Project res path: {args.res_root}\n")

    res_directories = find_res_directories(args.res_root)
    if not res_directories:
        print(f"Error: No 'res' directory found under {args.res_root}")
        return

    all_strings_files = {}
    for res_dir in res_directories:
        all_strings_files.update(find_strings_files(res_dir, args.strings_filename))

    if not all_strings_files:
        print(f"Error: No '{args.strings_filename}' files found in any 'res/values-*' directories under {args.res_root}.")
        return

    base_file_path = all_strings_files.get(args.base_lang)
    if not base_file_path:
        if args.base_lang != "default" and all_strings_files.get("default"):
            print(f"Warning: Base language '{args.base_lang}' not found. Using 'default' (values/{args.strings_filename}) as base.")
            base_file_path = all_strings_files.get("default")
            args.base_lang = "default"
        else:
            print(f"Error: Base strings file for language '{args.base_lang}' not found.")
            print(f"Available language files found: {list(all_strings_files.keys())}")
            return

    # If no key_name is provided, check only keys used in .java files
    if args.key_name is None:
        used_keys = find_used_keys_in_java(args.project_root)
        all_keys = set(get_all_keys(base_file_path))
        keys_to_check = sorted(list(all_keys & used_keys))
        if not keys_to_check:
            print('no keys to check')
            return
    else:
        keys_to_check = [args.key_name]

    total_issues_found = 0
    file_error_counts = {}
    file_warning_counts = {}

    for key_name in keys_to_check:
        base_value, base_specifiers = get_string_value_and_specifiers(base_file_path, key_name)
        if base_specifiers is None:
            continue

        for lang_code, file_path in all_strings_files.items():
            if lang_code == args.base_lang:
                continue

            current_value, current_specifiers = get_string_value_and_specifiers(file_path, key_name)
            if current_specifiers is None:
                continue

            error_count = 0
            warning_count = 0

            non_escaped_percent_indices = find_non_escaped_percent(current_value)
            if non_escaped_percent_indices:
                error_count += 1
                print(f"--- Language: {lang_code} (NON-ESCAPED % FOUND) ---")
                print(f"Key: {key_name}")
                print(f"File: {file_path}")
                print(f"Value: \"{current_value}\"")
                print(f"Non-escaped % at positions: {non_escaped_percent_indices}")
                print("-" * 20)

            if current_specifiers != base_specifiers:
                missing_in_current = base_specifiers - current_specifiers
                extra_in_current = current_specifiers - base_specifiers

                if extra_in_current:
                    error_count += 1
                    print(f"--- Language: {lang_code} (ISSUE FOUND) ---")
                    print(f"Key: {key_name}")
                    print(f"File: {file_path}")
                    print(f"Value: \"{current_value}\"")
                    print(f"Specifiers: {sorted(list(current_specifiers)) if current_specifiers else 'None'}")
                    print(f"Expected specifiers (from base): {sorted(list(base_specifiers)) if base_specifiers else 'None'}")
                    print(f"  EXTRA in '{lang_code}': {sorted(list(extra_in_current))}")
                    print("-" * 20)
                if missing_in_current:
                    warning_count += 1
                    print(f"--- Language: {lang_code} (WARNING: MISSING SPECIFIERS) ---")
                    print(f"Key: {key_name}")
                    print(f"File: {file_path}")
                    print(f"Value: \"{current_value}\"")
                    print(f"Specifiers: {sorted(list(current_specifiers)) if current_specifiers else 'None'}")
                    print(f"Expected specifiers (from base): {sorted(list(base_specifiers)) if base_specifiers else 'None'}")
                    print(f"  MISSING in '{lang_code}': {sorted(list(missing_in_current))}")
                    print("-" * 20)

            if error_count:
                file_error_counts[file_path] = file_error_counts.get(file_path, 0) + error_count
            if warning_count:
                file_warning_counts[file_path] = file_warning_counts.get(file_path, 0) + warning_count

    if file_error_counts or file_warning_counts:
        print("\nSummary of errors and warnings per file:")
        for file_path in sorted(set(list(file_error_counts.keys()) + list(file_warning_counts.keys()))):
            error_str = f"{file_error_counts.get(file_path, 0)} error(s)"
            warning_str = f"{file_warning_counts.get(file_path, 0)} warning(s)"
            print(f"{file_path}:\t {error_str:>5}, {warning_str:>5}")

        total_errors = sum(file_error_counts.values())
        total_warnings = sum(file_warning_counts.values())
        print(f"\nTOTAL: {total_errors} error(s), {total_warnings} warning(s)")

    if file_error_counts:
        exit(1)

if __name__ == "__main__":
    main()
