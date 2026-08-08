#!/usr/bin/env bash
# In-server regression test: boots a real NeoForge server, runs the CardboardTest
# probes from the console, and fails on any FAIL line.
#
# This is the counterpart to tools/rewriter_test.sh. That one proves the bytecode
# rewrite is correct in isolation; this one proves it actually reaches a
# precompiled plugin on a running server, alongside the rest of the
# cross-ecosystem behaviour.
#
# Requires the mod in <server-dir>/mods, CardboardTest.jar in
# <server-dir>/plugins, and the Waystones mods installed for the modded probes.
#
# Usage: regression_test.sh <server-dir>
set -uo pipefail

SERVER_DIR=${1:?usage: regression_test.sh <server-dir>}
LOG=$(mktemp)

echo "Booting $SERVER_DIR and running the probes..."

(
    for _ in $(seq 1 120); do
        grep -q 'Done (' "$LOG" 2>/dev/null && break
        sleep 1
    done
    sleep 3
    echo "cbtest mods"
    sleep 5
    echo stop
    sleep 25
) | (cd "$SERVER_DIR" && timeout 200 ./run.sh) > "$LOG" 2>&1

if ! grep -q 'Done (' "$LOG"; then
    echo "FAIL: server never reached Done -> $LOG"
    exit 1
fi

results=$(grep -E '\[PASS\]|\[FAIL\]|\[SKIP\]' "$LOG" | grep 'CardboardTest\]' | sed 's/.*CardboardTest\] //')
if [ -z "$results" ]; then
    echo "FAIL: the probe produced no output -> $LOG"
    exit 1
fi

echo "$results"
echo

fails=$(echo "$results" | grep -c '\[FAIL\]' || true)
errors=$(grep -cE '/ERROR\]|/FATAL\]' "$LOG" || true)

if [ "$fails" -ne 0 ] || [ "$errors" -ne 0 ]; then
    echo "FAIL: $fails failed probe(s), $errors server error(s) -> $LOG"
    exit 1
fi

rm -f "$LOG"
echo "PASS: all probes passed, no server errors."
