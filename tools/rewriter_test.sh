#!/usr/bin/env bash
# Offline regression test for the Material.values() call-site rewrite.
#
# Runs MaterialValuesRewriter over a real, precompiled plugin jar - the same
# bytes the server would load - and checks that every Material.values() call is
# redirected to the CardForge bridge with an unchanged descriptor, while every
# other Material call the plugin made is left exactly as it was.
#
# No server required, so this can run in CI. The in-server counterpart is
# `cbtest mods`, driven by tools/regression_test.sh.
#
# Usage: rewriter_test.sh <plugin.jar>
set -euo pipefail

JAR=${1:?usage: rewriter_test.sh <plugin.jar>}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
CLASSES="$ROOT/build/classes/java/main"

if [ ! -d "$CLASSES" ]; then
    echo "build the mod first: ./gradlew jar" >&2
    exit 2
fi

ASM=$(find "$HOME/.gradle/caches" -name 'asm-9.*.jar' 2>/dev/null | head -1)
if [ -z "$ASM" ]; then
    echo "could not locate asm on the gradle cache; run ./gradlew jar first" >&2
    exit 2
fi

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

javac -nowarn -cp "$CLASSES:$ASM" -d "$WORK" "$ROOT/tools/RewriterTest.java"
java -cp "$CLASSES:$ASM:$WORK" RewriterTest "$JAR"
