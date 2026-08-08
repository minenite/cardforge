# Porting Cardboard from Fabric to NeoForge

Measured against Cardboard `ver/26.2` (commit `c8d09f2d`) and NeoForge `26.2.x`.

## Why this is tractable

Cardboard's Bukkit implementation is almost entirely platform-agnostic. Of
**1,532 Java files, only 19 import `net.fabricmc.*`**. The Bukkit server
implementation, plugin loader, scheduler, command bridge, event system,
inventories and permissions are written against Minecraft classes, not
against Fabric.

That means this is not a rewrite. It is a platform-adapter swap plus a
build-system migration.

## The four real workstreams

### 1. Loader API — 19 files, small
| Fabric | Uses | NeoForge equivalent |
|---|---|---|
| `FabricLoader` | 11 | `ModList` / `FMLPaths` / `FMLLoader` |
| `EventFactory` / `Event` | 8 | NeoForge event bus |
| `ModInitializer` | 2 | `@Mod` + `FMLCommonSetupEvent` |
| `ModContainer` / `ModMetadata` | 3 | `IModInfo` |
| `MappingResolver` | 1 | not needed — both are unobfuscated on 26.2 |
| `KnotServer` / `FabricLauncherBase` | 2 | NeoForge launch handler |
| `fabric.api.screenhandler` | 3 | NeoForge menu registration |

The intended shape is a `PlatformAdapter` interface with a NeoForge
implementation, so the Bukkit layer never imports a loader API directly.

### 2. Mixins — 237 classes, mostly portable
NeoForge ships **Mixin 0.8.7 and MixinExtras 0.5.4** (`gradle.properties`),
the same stack Cardboard already targets. Mixin classes should carry across
largely unchanged. What changes is registration: `bukkitfabric.mixins.json`
moves to a NeoForge mixin config declared in `neoforge.mods.toml`.

Expect friction in the ~96 `@Overwrite`s where NeoForge itself already
patches the same method — NeoForge modifies far more of vanilla than Fabric
does, so overlapping patches are the main risk in this workstream.

### 3. Access widener → access transformer — done
Fabric access wideners have no NeoForge equivalent; NeoForge uses access
transformers. `tools/aw2at.py` performs the conversion:

```
914 AW entries -> 799 AT entries   (1 needs manual handling)
```

The one exception is `extend-enum DataFixTypes PAPER_NONE`, which has no AT
form and needs a different mechanism on NeoForge.

Regenerate with:
```
python3 tools/aw2at.py <cardboard>/src/main/resources/bukkitfabric.accesswidener \
    src/main/resources/META-INF/accesstransformer.cfg
```

### 4. iCommonLib — the actual blocker
Cardboard depends on iCommonLib in **18 files**, and iCommonLib is itself a
Fabric mod. It supplies cross-version abstractions Cardboard leans on:

```
me.isaiah.common.cmixin.IMixinEntity / IMixinWorld / IMixinMinecraftServer
me.isaiah.common.cmixin.IMixinChestBlockEntity / IMixinItemStack / IMixinGlobalPos
me.isaiah.common.entity.IRemoveReason
me.isaiah.common.event.block.LeavesDecayEvent / BlockEntityLoadEvent
me.isaiah.common.event.block.BlockEntityWriteNbtEvent
```

Two options:
- **Port iCommonLib to NeoForge** as well (it is small — the 26.2 module is
  ~28 source files), or
- **Absorb the interfaces** into Cardforge and drop the dependency, since
  most are thin bridge interfaces implemented by mixins anyway.

The second is likely cleaner for a NeoForge target: the cross-version
abstraction that justifies iCommonLib on Fabric buys little here.

## Build system

Loom is Fabric-only. NeoForge uses **ModDevGradle** (or NeoGradle).
`build.gradle` needs replacing wholesale, though the dependency set
(paper-api, Adventure 5, SpecialSource, Configurate, maven-resolver) carries
over unchanged.

Note `Libraries.java` downloads paper-api and Adventure at runtime with
pinned SHA1s. That mechanism is platform-independent and should be kept.

## Suggested order

1. ModDevGradle skeleton that compiles an empty mod against NeoForge 26.2
2. Access transformer in place (done)
3. Absorb or port the iCommonLib interfaces
4. Platform adapter + entrypoint, enough to reach `CraftServer` construction
5. Port mixin registration, then iterate on apply failures
6. Plugin loading, then command/event bridges
7. Run the CardboardTest probe plugin

## What this is not

None of the above is a working server yet. This document and the generated
access transformer are the starting point, not the port.
