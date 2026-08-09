#!/usr/bin/env bash
# In-server regression test: boots a real NeoForge server, runs the CardboardTest
# probes from the console, and fails on any FAIL line.
#
# Exit codes: 0 everything ran and passed, 1 something failed, 2 everything that
# ran passed but some probes were skipped for want of a connected player. 2 is
# deliberately not 0 - this harness once reported green while never executing the
# player-only probes at all, so "clean" and "complete" are now separate claims.
# Override with SKIP_EXIT_CODE=0 if a caller genuinely wants skips tolerated.
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
    # "auto" is both suites: the modded/cross-ecosystem probes and the core
    # Bukkit/Paper ones. This ran only "mods" at first, so the core suite was
    # built and then never executed by the harness meant to run it.
    echo "cbtest auto"
    sleep 10
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
skips=$(echo "$results" | grep -c '\[SKIP\]' || true)
errors=$(grep -cE '/ERROR\]|/FATAL\]' "$LOG" || true)

if [ "$fails" -ne 0 ] || [ "$errors" -ne 0 ]; then
    echo "FAIL: $fails failed probe(s), $errors server error(s) -> $LOG"
    exit 1
fi

# A skipped probe is not a passing probe. This harness drives the console, so the
# checks that need a player online cannot run here, and reporting the run as clean
# would restate the gap that let a whole category go unexercised while the suite
# stayed green. Report the count, and say plainly what it does not cover.
if [ "$skips" -ne 0 ]; then
    echo "INCOMPLETE: $fails failed, $skips skipped (no player online), no server errors."
    echo "$results" | grep '\[SKIP\]' | sed 's/^/  /'
    echo
    echo "These require a connected client. Run '/cbtest all' in game to cover them."
    echo "Log kept for inspection -> $LOG"
    exit "${SKIP_EXIT_CODE:-2}"
fi

rm -f "$LOG"
echo "PASS: all probes passed, nothing skipped, no server errors."
