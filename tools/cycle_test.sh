#!/usr/bin/env bash
# Repeated clean startup/shutdown cycles against a real NeoForge server.
#
# Boot failures are usually caught the first time, but state that leaks across
# a lifecycle - a static left set, a listener not unregistered, a world lock not
# released - only shows up on the second or third run. This drives full cycles
# through the console's own `stop` command rather than a signal, so the Bukkit
# shutdown path (plugin disable, world save, lock release) actually executes.
#
# Usage: cycle_test.sh <server-dir> [cycles]
set -uo pipefail

SERVER_DIR=${1:?usage: cycle_test.sh <server-dir> [cycles]}
CYCLES=${2:-3}
LOGDIR=$(mktemp -d)
FAILED=0

echo "Running $CYCLES start/stop cycle(s) against $SERVER_DIR"
echo "Logs: $LOGDIR"
echo

for i in $(seq 1 "$CYCLES"); do
    LOG="$LOGDIR/cycle-$i.log"

    # Hold stdin open so the server keeps running, then send `stop` once it is up.
    (
        for _ in $(seq 1 120); do
            grep -q 'Done (' "$LOG" 2>/dev/null && break
            sleep 1
        done
        sleep 2
        echo stop
        sleep 25
    ) | (cd "$SERVER_DIR" && timeout 180 ./run.sh) > "$LOG" 2>&1

    ok=1
    reason=""
    grep -q 'Done (' "$LOG"                  || { ok=0; reason="never reached Done"; }
    grep -q 'Enabling CardboardTest' "$LOG"  || { ok=0; reason="${reason:+$reason; }plugin not enabled"; }
    grep -q 'Disabling CardboardTest' "$LOG" || { ok=0; reason="${reason:+$reason; }plugin not disabled"; }
    grep -q 'Stopping server' "$LOG"         || { ok=0; reason="${reason:+$reason; }no clean shutdown"; }
    if grep -qE '/ERROR\]|/FATAL\]' "$LOG"; then
        ok=0
        reason="${reason:+$reason; }errors logged"
    fi

    if [ "$ok" = 1 ]; then
        echo "cycle $i: PASS"
    else
        echo "cycle $i: FAIL ($reason)  -> $LOG"
        grep -E '/ERROR\]|/FATAL\]' "$LOG" | head -5
        FAILED=1
    fi
done

echo
if [ "$FAILED" = 0 ]; then
    echo "All $CYCLES cycle(s) passed."
else
    echo "One or more cycles failed."
fi
exit "$FAILED"
