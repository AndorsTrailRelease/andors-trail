#!/usr/bin/env bash
set -eu

if [[ "${CODESPACES:-false}" == "true" ]]; then
    gradle_user_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
    gradle_properties="$gradle_user_home/gradle.properties"
    mkdir -p "$gradle_user_home"
    touch "$gradle_properties"

    if grep -Eq '^[[:space:]]*org[.]gradle[.]workers[.]max([[:space:]]|[=:]|$)' "$gradle_properties"; then
        printf 'Preserving existing Gradle worker setting in %s\n' "$gradle_properties"
    else
        grep_status=$?
        if [[ "$grep_status" -ne 1 ]]; then
            printf 'Failed to read %s\n' "$gradle_properties" >&2
            exit "$grep_status"
        fi
        printf '\n# Limit build memory pressure in GitHub Codespaces.\norg.gradle.workers.max=4\n' >> "$gradle_properties"
        printf 'Initialized Gradle worker limit in %s\n' "$gradle_properties"
    fi
fi
