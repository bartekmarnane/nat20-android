#!/usr/bin/env bash
# Cloud environment setup for Claude Code on the web (nat20-android).
# Configure this as the environment's setup script:  bash scripts/web-setup.sh
#
# Each web session clones only this repo. This script makes the session self-contained:
#   1. clones the iOS reference repo as a sibling (so CLAUDE.md's ../nat20-ios paths resolve)
#   2. installs the Android SDK (only needed from step A4 onward; A1-A3 build on the JDK alone)
#   3. writes local.properties so Gradle can find the SDK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 1) iOS reference (README = feature spec; Packages/Domain + Packages/DnD5e = behavioural spec)
if [ ! -d "$REPO_ROOT/../nat20-ios" ]; then
  git clone --depth 1 https://github.com/bartekmarnane/nat20-ios.git "$REPO_ROOT/../nat20-ios"
fi

# 2) Android SDK — only needed from A4 (:app/:data). A1-A3 (:domain, :ruleset-dnd5e) need only
#    the pre-installed JDK 21 + Gradle, so the port can start even if this part needs tuning.
#    NOTE: requires dl.google.com to be reachable. If the environment uses "Trusted" network and
#    the download fails, switch to "Custom" and allow dl.google.com, then re-run. Verify in a
#    trial session before relying on it.
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdline-tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q "$tmp/cmdline-tools.zip" -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
fi

# 3) Point Gradle at the SDK (local.properties is gitignored, absent in a fresh clone)
echo "sdk.dir=$ANDROID_HOME" > "$REPO_ROOT/local.properties"

echo "web-setup complete: iOS reference at ../nat20-ios, ANDROID_HOME=$ANDROID_HOME"
