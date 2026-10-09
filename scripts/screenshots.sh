#!/bin/bash
#
# Screenshot capture (the Android analogue of ../nat20-ios/Scripts/screenshots.sh).
#
# Builds the DEBUG app, installs it fresh on the connected emulator/device, and
# launches it once per shot with `--es shot <name>` — the DEBUG-only harness
# (`ui/ScreenshotRoute.kt` + the router in `NatApp.kt`) skips splash/onboarding
# and navigates straight to the screen, seeding a campaign for the demo
# character where needed. One launch per shot keeps every frame reproducible
# without driving taps.
#
# Usage:  scripts/screenshots.sh [shot ...]   (default: every shot, light + dark)
#   NAT20_SCREENSHOT_OUT=screenshots/pixel-8   output dir (dark set gets "-dark")
#   NAT20_SCREENSHOT_MODES="light"             restrict appearance modes
#   NAT20_SCREENSHOT_DELAY=5                   seconds to settle before the shutter
#
# `emptyRoster` deletes the seeded roster for the rest of the install, so it
# runs last in the default order; `journal` / `stats` etc. start a campaign for
# Lyra (sticky for the install) and `past` ends one for Thorgar.
set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG="au.com.evonet.nat20"
OUT_DIR="${NAT20_SCREENSHOT_OUT:-screenshots/pixel-8}"
MODES=(${NAT20_SCREENSHOT_MODES:-light dark})
SHOTS=(roster stats skills combat spells items lore journal actions levelUp
       onboarding settings patron credits
       spellLibrary itemCatalog monsterCodex customCreatures
       create building editor characterSettings contentSources past
       sheet2024 combat2024 spells2024 items2024 journal2024 sheetPF2e
       emptyRoster)
if [ "$#" -gt 0 ]; then SHOTS=("$@"); fi

say() { printf '\033[1;36m==>\033[0m %s\n' "$1"; }

say "Building (debug)"
./gradlew :app:assembleDebug -q --console=plain
APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || { echo "No APK at $APK" >&2; exit 1; }

say "Installing fresh"
$ADB uninstall "$PKG" >/dev/null 2>&1 || true
$ADB install -r "$APK" >/dev/null

# Demo status bar: 9:41, full bars, full battery, no notifications.
$ADB shell settings put global sysui_demo_allowed 1 >/dev/null
demo() { $ADB shell am broadcast -a com.android.systemui.demo "$@" >/dev/null 2>&1 || true; }
demo -e command enter
demo -e command clock -e hhmm 0941
demo -e command battery -e level 100 -e plugged false
demo -e command network -e wifi show -e level 4 -e mobile hide
demo -e command notifications -e visible false

for mode in "${MODES[@]}"; do
    if [ "$mode" = "dark" ]; then
        $ADB shell cmd uimode night yes >/dev/null
        dir="$OUT_DIR-dark"
    else
        $ADB shell cmd uimode night no >/dev/null
        dir="$OUT_DIR"
    fi
    mkdir -p "$dir"
    index=0
    for shot in "${SHOTS[@]}"; do
        index=$((index + 1))
        label=$(printf '%02d-%s' "$index" "$shot")
        say "Shot $mode/$label"
        $ADB shell am force-stop "$PKG"
        $ADB shell am start -W -n "$PKG/.MainActivity" --es shot "$shot" >/dev/null
        sleep "${NAT20_SCREENSHOT_DELAY:-5}"
        $ADB exec-out screencap -p > "$dir/$label.png"
    done
done

$ADB shell am force-stop "$PKG"
demo -e command exit
$ADB shell cmd uimode night no >/dev/null
say "Done — $OUT_DIR"
