#!/usr/bin/env bash
# CardForge installer.
#
# Sets up a NeoForge 26.2 dedicated server with the CardForge compatibility layer
# installed, so Bukkit/Spigot/Paper plugins run alongside NeoForge mods.
#
# CardForge is not a fork of NeoForge and does not ship it. This fetches the
# official NeoForge installer and runs it, then drops CardForge into mods/ - so
# the result is a normal NeoForge server that also happens to load plugins.
#
# Usage: ./install.sh [target-directory]
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
TARGET=${1:-$(pwd)}
NEOFORGE_VERSION=$(cat "$HERE/neoforge-version.txt")
INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar"

say() { printf '\033[1m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

command -v java >/dev/null || die "java not found on PATH. CardForge 26.2 needs Java 21 or newer."

JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
    die "Java $JAVA_MAJOR found; CardForge 26.2 needs Java 21 or newer."
fi

mkdir -p "$TARGET"
cd "$TARGET"

if [ -f "libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/unix_args.txt" ]; then
    say "NeoForge ${NEOFORGE_VERSION} already installed here, skipping."
else
    say "Downloading NeoForge ${NEOFORGE_VERSION} installer..."
    if command -v curl >/dev/null; then
        curl -fL# -o neoforge-installer.jar "$INSTALLER_URL"
    elif command -v wget >/dev/null; then
        wget -q --show-progress -O neoforge-installer.jar "$INSTALLER_URL"
    else
        die "need curl or wget to download the NeoForge installer"
    fi

    say "Running the NeoForge server installer..."
    java -jar neoforge-installer.jar --installServer . || die "NeoForge installer failed"
    rm -f neoforge-installer.jar neoforge-installer.jar.log
fi

say "Installing CardForge..."
mkdir -p mods plugins
cp "$HERE"/mods/*.jar mods/

# The API artifact is for plugin authors, not for the server: putting it in mods/
# or plugins/ would do nothing useful, so it goes somewhere obvious instead.
mkdir -p cardforge-api
cp "$HERE"/api/*.jar cardforge-api/ 2>/dev/null || true
cp "$HERE"/README.md . 2>/dev/null || true

if [ ! -f eula.txt ]; then
    cat > eula.txt <<'EULA'
# Set eula=true to accept the Minecraft EULA at https://aka.ms/MinecraftEULA
eula=false
EULA
    say "Wrote eula.txt - set eula=true to accept the Minecraft EULA before starting."
fi

cat <<DONE

CardForge is installed in: $TARGET

  mods/       NeoForge mods, plus Cardforge itself
  plugins/    Bukkit / Spigot / Paper plugins, and CardForge-native plugins
  cardforge-api/  compile against this to write a CardForge-native plugin

Next:
  1. set eula=true in eula.txt
  2. drop your mods into mods/ and your plugins into plugins/
  3. ./run.sh

DONE
