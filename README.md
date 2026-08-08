# CardForge

Run Bukkit, Spigot and Paper plugins on a real NeoForge 26.2 server, alongside
your NeoForge mods.

```
NeoForge 26.2 (the real dedicated server)
    -> CardForge (Cardboard's Bukkit/Spigot/Paper implementation, ported)
        -> your plugins
```

CardForge is not a fork of NeoForge or of Paper, and it does not port CraftBukkit
again. It loads as an ordinary NeoForge mod and brings the Bukkit API up on top
of the server NeoForge is already running, reusing
[Cardboard](https://github.com/minenite/cardboard)'s implementation. Mods stay
normal NeoForge mods; plugins stay normal server-side plugins.


## Where CardForge sits

| Approach | Deep Modding | Bukkit/Paper Plugins | Mod↔Plugin Integration | Overall Flexibility |
| --- | --- | --- | --- | --- |
| Vanilla/Paper | ❌ | ✅ Excellent | ❌ | 🟡 |
| Fabric | ✅ | ❌ | ❌ | 🟢 |
| NeoForge | ✅ Excellent | ❌ | ❌ | 🟢 |
| Cardboard | ✅ Fabric | ✅ | ⚠️ Limited | 🟢 |
| CardForge | ✅ NeoForge | ✅ | ✅ Designed for it | 🟢🟢 Excellent |

The last column is the point. Running both ecosystems side by side is table
stakes; what CardForge adds is that they can see each other - a Bukkit plugin
can address modded content by its real namespaced id, cancel a mod's operation,
and a CardForge-native plugin can reach NeoForge capabilities directly. See
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for what that is verified to mean.

## For server owners

Grab a release and run the installer — you do not need this repository, Gradle,
or a source checkout:

```sh
./install.sh /path/to/server
```

That fetches the official NeoForge installer, runs it, and puts CardForge in
`mods/`. See [docs/DISTRIBUTION.md](docs/DISTRIBUTION.md) for the full walkthrough.

## For plugin developers

Existing Bukkit/Paper plugins need no changes. If you want a plugin that is
deliberately NeoForge-aware — enumerating loaded mods, addressing modded content
by its real namespaced id, or reading a modded machine's inventory through
NeoForge capabilities — compile against the API artifact and opt in:

```java
CardForge.getIfPresent().ifPresent(cardforge -> {
    if (cardforge.isModLoaded("waystones")) {
        // integrate
    }
});
```

Guarded like that, the same jar still runs on plain Paper. See
[CardForgeExample](https://github.com/minenite/CardForgeExample) for a working
plugin that demonstrates each part of the API.

## Status

Working and tested server-side. Strict Mixin behaviour is enabled
(`defaultRequire: 1`), the server boots clean with zero failed injections,
external plugins complete their full lifecycle, real NeoForge mods load
alongside them, and modded content crosses into the Bukkit API.

Not verified: anything requiring a connected client. See
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md), which records what was actually
run, what still fails, and why — including the five core probes that do not pass.

## Building

```sh
./gradlew jar        # the mod
./gradlew apiJar     # the slim API artifact for plugin authors
./gradlew dist       # the distributable zip
```

## Tests

```sh
python3 tools/check_class_overlap.py build/libs/Cardforge-26.2.jar <server-dir>
python3 tools/audit_overlap.py src/main/java <neoforge-repo>
tools/rewriter_test.sh <plugin.jar>
tools/regression_test.sh <server-dir>
tools/cycle_test.sh <server-dir> 3
```

- `check_class_overlap.py` — fails if the jar ships a class NeoForge already
  provides, which becomes a `LinkageError` the moment it crosses a classloader.
- `audit_overlap.py` — cross-references Cardboard's mixin targets against
  NeoForge's patches, so a hook that still applies but no longer means the same
  thing gets reported.
- `rewriter_test.sh` — proves the `Material.values()` call-site rewrite is
  correct and touches nothing else.
- `regression_test.sh` — boots a real server and runs the probe suite.
- `cycle_test.sh` — repeated start/stop cycles.

## Documentation

- [docs/DISTRIBUTION.md](docs/DISTRIBUTION.md) — installing and running
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) — what works, what does not, how it was established
- [docs/PORTING.md](docs/PORTING.md) — how the Fabric implementation was moved onto NeoForge
