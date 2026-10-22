#!/usr/bin/env bash
set -eu

sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
mkdir -p "$ANDROID_HOME/cmdline-tools"

if [[ ! -x "$sdkmanager" ]]; then
    temp_dir="$(mktemp -d)"
    trap 'rm -rf "$temp_dir"' EXIT

    # pinned version of the Android command line tools that allow for auto-acceptance of licenses
    archive_url="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
    curl -fsSL "$archive_url" -o "$temp_dir/cmdline-tools.zip"
    mkdir "$temp_dir/unpacked"
    unzip -q "$temp_dir/cmdline-tools.zip" -d "$temp_dir/unpacked"
    mv "$temp_dir/unpacked/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

yes | "$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses