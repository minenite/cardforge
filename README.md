# Cardforge

Running Bukkit/Spigot/Paper plugins on **NeoForge** by reusing
[Cardboard](https://github.com/minenite/cardboard)'s Bukkit implementation
instead of porting CraftBukkit again.

```
NeoForge 26.2  ->  Cardboard's Bukkit compatibility layer  ->  Bukkit/Spigot/Paper plugins
```

## Status

**Early. This does not build a working server yet.**

What exists today is the groundwork: a measured porting plan, the access
transformer generated from Cardboard's access widener, and the platform seam
that the loader-specific code will sit behind.

| Workstream | State |
|---|---|
| Access widener → access transformer | done — 799 entries generated, 1 manual |
| Porting plan, measured against both trees | done |
| Platform adapter seam | interface defined |
| ModDevGradle build | not started |
| iCommonLib dependency | not resolved |
| Mixin migration (237 classes) | not started |
| Bukkit layer running on NeoForge | not started |

## Why reuse Cardboard

Of Cardboard's **1,532 Java files, only 19 import `net.fabricmc.*`**. The
plugin loader, Bukkit server implementation, scheduler, commands, events,
inventories and permissions are written against Minecraft, not Fabric. The
platform-specific surface is small enough to swap.

NeoForge also ships the same Mixin stack Cardboard already uses
(Mixin 0.8.7, MixinExtras 0.5.4), so the 237 mixin classes are expected to
port with configuration changes rather than rewrites.

See [docs/PORTING.md](docs/PORTING.md) for the full breakdown and the
suggested order of work.

## Tools

```
python3 tools/aw2at.py <cardboard>/src/main/resources/bukkitfabric.accesswidener \
    src/main/resources/META-INF/accesstransformer.cfg
```

Converts a Fabric access widener into a NeoForge access transformer and
reports anything with no AT equivalent rather than dropping it silently.

## Credits

Cardboard is by [CardboardPowered](https://github.com/CardboardPowered/cardboard)
and inherits Paper's license. NeoForge is by [NeoForged](https://github.com/neoforged/NeoForge).
