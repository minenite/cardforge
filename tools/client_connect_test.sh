#!/usr/bin/env bash
# Launches a real NeoForge client and auto-connects it to a CardForge server.
#
# This exercises the parts of the stack no headless test reaches: the NeoForge
# channel handshake, the mod-list compatibility check the client and server run
# against each other, the login sequence, and whether CardForge's presence
# disturbs any of it.
#
# It uses --quickPlayMultiplayer so no GUI driving is needed: the client boots
# straight into the connection. The server must be in offline mode, because this
# has no Mojang session; everything being tested here happens after auth.
#
# Usage: client_connect_test.sh <host:port> [minecraft-dir]
set -uo pipefail

TARGET=${1:?usage: client_connect_test.sh <host:port> [minecraft-dir]}
MCDIR=${2:-$HOME/.minecraft}
NEO_ID=$(ls "$MCDIR/versions" | grep -m1 '^neoforge-' || true)
[ -n "$NEO_ID" ] || { echo "no neoforge client installed under $MCDIR/versions"; exit 2; }

PARENT=$(python3 -c "
import json;print(json.load(open('$MCDIR/versions/$NEO_ID/$NEO_ID.json'))['inheritsFrom'])")

echo "client:  $NEO_ID (inherits $PARENT)"
echo "server:  $TARGET"

CP=$(python3 - "$MCDIR" "$NEO_ID" "$PARENT" <<'PY'
import json, os, sys
mcdir, neo, parent = sys.argv[1], sys.argv[2], sys.argv[3]
entries, seen = [], set()

def add(libs):
    for lib in libs:
        name = lib.get('name')
        if not name or name in seen:
            continue
        # Honour the rules block so we do not pull in other platforms' natives.
        allow = True
        for rule in lib.get('rules', []):
            osname = rule.get('os', {}).get('name')
            match = osname is None or osname == 'linux'
            allow = match if rule.get('action') == 'allow' else (not match if match else allow)
        if not allow:
            continue
        seen.add(name)
        group, artifact, version = name.split(':')[:3]
        path = os.path.join(mcdir, 'libraries', *group.split('.'), artifact, version,
                            f'{artifact}-{version}.jar')
        if os.path.isfile(path):
            entries.append(path)

for v in (neo, parent):
    with open(os.path.join(mcdir, 'versions', v, f'{v}.json')) as fh:
        add(json.load(fh).get('libraries', []))

entries.append(os.path.join(mcdir, 'versions', parent, f'{parent}.jar'))
print(':'.join(entries))
PY
)

echo "classpath entries: $(tr ':' '\n' <<<"$CP" | wc -l)"

ASSETS_INDEX=$(python3 -c "
import json;print(json.load(open('$MCDIR/versions/$PARENT/$PARENT.json'))['assetIndex']['id'])")

# A throwaway offline identity. The server must be offline-mode for this to pass.
exec java -Xmx2G \
    -Djava.library.path="$MCDIR/versions/$PARENT/natives" \
    -Dfml.earlyprogresswindow=false \
    -cp "$CP" \
    net.neoforged.fml.startup.Client \
    --username CardForgeTest \
    --uuid 00000000000040008000000000000001 \
    --accessToken 0 \
    --version "$NEO_ID" \
    --gameDir "$MCDIR" \
    --assetsDir "$MCDIR/assets" \
    --assetIndex "$ASSETS_INDEX" \
    --userType legacy \
    --quickPlayMultiplayer "$TARGET"
