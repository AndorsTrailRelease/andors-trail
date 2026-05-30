#!/usr/bin/env python3
# Script to change all printf-style integer placeholders from "%1$d" to "%1$,d" (i.e., add a localized separator)
# This is intended to be used ONCE during migration; after that, all new strings should be added with the comma already in place.
# (although it is smart enough not to add a comma if one is already there, so it can be safely re-run if needed)
#
# Usage: python3 convert-int-placeholders-in-strings-xml.py <strings.xml> [...]
# To apply to all strings files in the project, execute from the project root:
#    python3 tools/convert-int-placeholders-in-strings-xml.py ./res/values*/strings.xml
#

import re
import sys

# Pattern to match integer placeholders like %1$,d %2$,d etc. (but not already with a comma)
PLACEHOLDER_PATTERN = re.compile(r"%(\d+)\$d")

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace %1$d with %1$,d
    new_content, count = PLACEHOLDER_PATTERN.subn(r"%\1$,d", content)
    if count > 0:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {path} ({count} replacements)")
    else:
        print(f"No changes in {path}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 convert-int-placeholders-in-strings-xml.py <strings.xml> [more ...]")
        sys.exit(1)
    for file in sys.argv[1:]:
        process_file(file)

if __name__ == "__main__":
    main()
