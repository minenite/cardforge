#!/usr/bin/env bash
#
# Install the built jar into a server's mods/ directory.
#
# This exists because `cp build/libs/Cardforge-26.2.jar <server>/mods/` is unsafe
# against a running server, in a way that produces a bug report nobody can read.
# cp truncates and rewrites the file in place, keeping the same inode. A running
# JVM holds that zip open with the central directory it read at startup, so every
# cached entry offset now points into unrelated bytes. The next class the server
# loads - and only classes not yet loaded, which is why it looks intermittent -
# fails with:
#
#     java.lang.NoClassDefFoundError: org/bukkit/craftbukkit/event/CraftEventFactory
#     Caused by: java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)
#
# The class is present in the jar, which sends you looking for a packaging or
# classloader fault that does not exist. This was reproduced deliberately: boot
# clean (94 probes passing), cp a jar with shifted entry offsets over it, and the
# failure appears on the next cold class load.
#
# Two defences here. Refuse outright while the server is running, and when it is
# not, install atomically via rename so a server that starts mid-copy either sees
# the whole old jar or the whole new one, never a half-written file.
set -euo pipefail

usage() {
    echo "usage: $0 [--plugin] <server-dir> [jar]" >&2
    echo "  jar defaults to build/libs/Cardforge-26.2.jar" >&2
    echo "  --plugin installs into plugins/ instead of mods/" >&2
    exit 2
}

# Plugin jars are held open by the plugin classloader exactly as the mod jar is
# held by the module classloader, so they carry the same hazard and get the same
# protection.
SUBDIR=mods
if [ "${1:-}" = "--plugin" ]; then
    SUBDIR=plugins
    shift
fi

[ $# -ge 1 ] || usage

SERVER_DIR="${1%/}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${2:-$REPO_ROOT/build/libs/Cardforge-26.2.jar}"

[ -d "$SERVER_DIR" ]      || { echo "error: no such server directory: $SERVER_DIR" >&2; exit 1; }
[ -d "$SERVER_DIR/$SUBDIR" ] || { echo "error: not a server directory (no $SUBDIR/): $SERVER_DIR" >&2; exit 1; }
[ -f "$JAR" ]             || { echo "error: no such jar: $JAR" >&2; exit 1; }

# A jar that is not a readable zip must never reach mods/; the server would fail
# to load the mod at boot, which is at least loud, but there is no reason to ship
# a corrupt file when checking costs nothing.
unzip -qt "$JAR" >/dev/null 2>&1 || { echo "error: $JAR is not a valid zip archive" >&2; exit 1; }

TARGET="$SERVER_DIR/$SUBDIR/$(basename "$JAR")"

# Is a JVM holding this server open? Match on the resolved server directory rather
# than a process-name pattern: several unrelated things on a dev box have
# "neoforge" in their command line, including the Minecraft client, and killing or
# skipping based on that would be wrong in both directions.
running_pids() {
    local abs
    abs="$(cd "$SERVER_DIR" && pwd)"
    for pid in $(pgrep -u "$(id -u)" java 2>/dev/null || true); do
        # cwd is the reliable signal: the server runs from its own directory.
        if [ "$(readlink -f "/proc/$pid/cwd" 2>/dev/null || true)" = "$abs" ]; then
            echo "$pid"
        fi
    done
}

PIDS="$(running_pids || true)"
if [ -n "$PIDS" ]; then
    echo "error: a server is running from $SERVER_DIR (pid: $(echo "$PIDS" | tr '\n' ' '))" >&2
    echo "" >&2
    echo "Overwriting the jar now would corrupt the running JVM's view of it and" >&2
    echo "produce NoClassDefFoundError on classes that are present in the jar." >&2
    echo "Stop the server first, then re-run this command." >&2
    exit 1
fi

# Same filesystem as the target, so the rename below is atomic.
TMP="$(mktemp "$SERVER_DIR/$SUBDIR/.deploy.XXXXXX")"
trap 'rm -f "$TMP"' EXIT
cp "$JAR" "$TMP"
chmod 644 "$TMP"
mv -f "$TMP" "$TARGET"
trap - EXIT

echo "deployed $(basename "$JAR") -> $TARGET"
if unzip -p "$TARGET" cardforge-build.properties 2>/dev/null | sed 's/^/  /'; then
    :
elif unzip -p "$TARGET" META-INF/MANIFEST.MF 2>/dev/null | grep -E '^Build-(Commit|Branch|Dirty|Time):' | sed 's/^/  /' | grep -q .; then
    # Plugin jars stamp the manifest rather than shipping a properties file.
    unzip -p "$TARGET" META-INF/MANIFEST.MF 2>/dev/null | grep -E '^Build-(Commit|Branch|Dirty|Time):' | sed 's/^/  /'
else
    echo "  (no build stamp in this jar)"
fi
