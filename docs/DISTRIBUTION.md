# CardForge 26.2

Run Bukkit, Spigot and Paper plugins on a real NeoForge 26.2 server, alongside
your NeoForge mods.

CardForge is not a fork of NeoForge or of Paper. It is a compatibility subsystem
that loads as an ordinary NeoForge mod and brings up the Bukkit/Spigot/Paper API
on top of the server NeoForge is already running:

```
NeoForge 26.2 (the real dedicated server)
    -> CardForge (the Bukkit/Spigot/Paper implementation)
        -> your plugins
```

Your mods stay normal NeoForge mods. Your plugins stay normal server-side
plugins. Neither has to know the other exists.

## Install

```sh
./install.sh /path/to/server
```

That downloads the official NeoForge installer, runs it, and puts CardForge in
`mods/`. You need Java 21 or newer. Nothing else — no Gradle, no source
checkout, no patching.

Then:

1. Set `eula=true` in `eula.txt` to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA).
2. Put your mods in `mods/` and your plugins in `plugins/`.
3. `./run.sh`

## Layout

```
your-server/
├── mods/            NeoForge mods, and Cardforge itself
├── plugins/         Bukkit / Spigot / Paper plugins, and CardForge-native ones
├── cardforge-api/   compile against this to write a CardForge-native plugin
├── libraries/       NeoForge's own files, from its installer
├── world/
└── run.sh
```

Mod dependencies go in `mods/` exactly as they would on any NeoForge server.
Plugin dependencies go in `plugins/`, and a plugin's `libraries:` block in
`plugin.yml` is resolved for you.

## Writing a CardForge-native plugin

Ordinary Bukkit plugins need no changes and know nothing about NeoForge. If you
want a plugin that is deliberately NeoForge-aware — one that can enumerate loaded
mods, address modded content by its real namespaced id, or read a modded
machine's inventory through NeoForge capabilities — compile against
`cardforge-api/Cardforge-API-26.2.jar` and opt in:

```java
CardForge.getIfPresent().ifPresent(cardforge -> {
    if (cardforge.isModLoaded("waystones")) {
        // integrate
    }
});
```

Keep it guarded like that and the same jar still runs on a plain Paper server.

See `docs/COMPATIBILITY.md` for what is verified to work, what is not, and why.

## Reporting problems

Include your `logs/latest.log`, the contents of `mods/` and `plugins/`, and
whether the problem also happens with the plugin or mod removed. Knowing which
side a failure comes from is most of the work.
