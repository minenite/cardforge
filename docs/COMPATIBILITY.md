# CardForge 26.2 compatibility report

What is verified, what is known-broken, and how it was established. Everything
below was observed on a real dedicated NeoForge 26.2.0.52-beta server with strict
Mixin behaviour enabled (`defaultRequire: 1` on both configs), not inferred from
the code.

## Architecture

CardForge is a compatibility subsystem running **on** a real NeoForge server, not
a reimplementation of one:

```
NeoForge 26.2 (real dedicated server)
    -> CardForge (Cardboard's Bukkit/Spigot/Paper implementation, ported)
        -> Bukkit / Spigot / Paper plugins
```

NeoForge mods load through NeoForge exactly as they normally do. CardForge adds
the Bukkit layer alongside them and bridges content between the two.

## Verified working

| Area | Evidence |
| --- | --- |
| Strict Mixin application | Boots with `defaultRequire: 1`, zero failed injections |
| Server startup | Reaches `Done`, no ERROR or FATAL lines |
| Repeated lifecycles | 3 consecutive start/stop cycles, clean shutdown each time |
| External Bukkit plugin | Discovered, loaded, enabled, `onEnable` run, command registered, disabled on shutdown |
| Coexistence with NeoForge mods | Balm, Shogi and Waystones load alongside the Bukkit layer, zero errors |
| Modded blocks in the Bukkit API | 32 Waystones blocks registered as `Material`s |
| Modded `Material` lookup | `Material.getMaterial("WAYSTONES_ANDESITE_WAYSTONE")` resolves |
| Modded `NamespacedKey` | `Material#getKey()` returns `waystones:andesite_waystone` |
| Modded items | `new ItemStack(WAYSTONES_BOUND_SCROLL)` succeeds |
| Cross-ecosystem block write/read | A plugin places a modded block through the Bukkit API and reads it back |
| Modded block entities | `Block#getState()` on a modded block entity returns a usable `TileState` |
| Paper type registries | `Registry.ITEM` / `Registry.BLOCK` resolve modded ids; item-only ids correctly return `null` from `Registry.BLOCK` |
| `Material.values()` in plugins | A precompiled plugin iterating `values()` sees 2204 entries: vanilla plus 50 Waystones materials, no duplicates |
| Lookup behaviour unchanged | `valueOf`, `getMaterial`, `matchMaterial`, `Registry.MATERIAL`, `getKey`, `isBlock` all behave as before, and an unknown name still returns `null` |
| Core Bukkit/Paper behaviour | 84 passing probes across worlds, blocks, entities, projectiles, ItemStack/components, PDC, scoreboards, boss bars, scheduler, permissions, commands, configuration, registries and world saving |
| Integration API | 8 mods enumerated, modded content resolved by real namespaced id, NeoForge capabilities reachable |
| Distributable | `install.sh` fetches and runs the official NeoForge installer, then drops CardForge into `mods/` |

The plugin used is `CardboardTest`, whose `cbtest auto` runs from the console so
the whole check can execute in an automated boot with no client attached.

## Verified with a real NeoForge client

A real NeoForge 26.2.0.52-beta client, with Balm, Shogi and Waystones installed,
connected to a CardForge server running the same mods plus two plugins. Confirmed
in game:

| Area | Result |
| --- | --- |
| NeoForge handshake and mod-list compatibility check | Passes; CardForge itself is server-side only and not required client-side |
| Login, two accounts, PvP, `/tp` | Works |
| Modded GUI (Waystones naming and warp screens) | Opens and functions |
| Block place and break, vanilla and modded | Works, no warnings |
| Shearing, by player and by dispenser | Wool drops immediately |
| Entity damage and death in game | Events fire |
| Waystone teleport, including Nether to Overworld | Works |
| NeoForge capabilities through the CardForge API | `WorldlyContainerWrapper` resolved on a Waystone |
| Custom inventory GUI, click and close events | Works |

**This session found six bugs that the automated suite could not.** Three made the
server unusable - nobody could log in, clients were rejected for not having a
server-side mod, and modded GUIs killed the packet handler. The other three were
worse for being quiet: `BlockPlaceEvent` had never fired for any block and an
overly broad catch downgraded it to a warning; shear drops were being swallowed
into the death-drop list; and `rayTraceBlocks` - the basis of
`getTargetBlock`/`getTargetBlockExact` - had two independent faults and had never
once succeeded.

Every one of them sat in an area the console suite structurally cannot reach:
there is no handshake, no client, no player, and no one looking at a block. A
passing headless suite is necessary and nowhere near sufficient.

## Known limitations

These are real and are listed rather than papered over. Each is covered by a
probe in `CardboardTest`, so they fail loudly rather than rotting quietly.

### Six core probes still fail

Run `cbtest core` from the console to reproduce.

| Probe | Symptom | Assessment |
| --- | --- | --- |
| `entities: EntityDamageEvent` | `LivingEntity#damage()` reduces health but fires no event | Real. The **in-game** damage path is fine - a player hitting a mob fires it, verified by playtest - but the plugin-API entry point bypasses the hook. Matters because a plugin calling `damage()` expects other plugins to see and cancel it. |
| `entities: EntityDeathEvent` | `setHealth(0)` kills but fires no event | Real, same shape as above. In-game deaths fire it correctly. |
| `entities: teleport` | `Entity#teleport` does not move a mob | Real. Player teleport works, including cross-dimension via Waystones. |
| `itemstacks: serialize` | `ItemStack.serialize()` throws NPE, `craftDelegate` is null | Paper's ItemStack delegate is not wired for plugin-constructed stacks |
| `recipes: iterator` | `Invalid recipe type: DyeRecipe` | 26.2 added a recipe type Cardboard's converter does not map |
| `bossbars: keyed` | `CustomBossEvent` cannot be cast to `EntityBridge` | `Bukkit.createBossBar(key, ...)` reuses an entity bridge a boss event is not |

None are NeoForge-specific; they are Cardboard/26.2 porting gaps.

An earlier version of this document dismissed the first three as artefacts of
testing a freshly spawned entity, on the strength of a playtest showing damage
and death working in game. That was wrong twice over: the probe now waits for the
entity to be live and they still fail, and the playtest exercised the in-game
path while the probe exercises the plugin API. Two different paths, and only one
of them works. The lesson is that "verified by playtest" and "verified by probe"
are not interchangeable evidence - each covers what the other cannot.

## How `Material.values()` works

Modded materials are added by writing Material's private static final `$VALUES`
array through Unsafe. The write lands - reading the field reflectively shows every
modded entry - but `Material.values()` compiles to a `getstatic` on a static final
field, which HotSpot constant-folds once the class is initialised. `values()` is
hot during registration, so it folds early and then keeps returning the
pre-extension array. Nothing done at the read site changes that, because the fold
has already happened before any plugin runs.

So the read site is not where this is solved. `MaterialValuesRewriter` rewrites
plugin classes as they load, redirecting

```
invokestatic org/bukkit/Material.values()[Lorg/bukkit/Material;
```

to `CardForgeMaterials.values()`, which has the identical descriptor. That makes
it a drop-in substitution: precompiled plugin jars work unchanged, with no source
changes and no recompilation.

It is wired into two paths, because plugins do not all load the same way:
Paper plugins go through `PaperClassloaderBytecodeModifier`, and legacy Bukkit
plugins through `PluginClassLoader#findClass`. On the legacy path it has to run
**before** Cardboard's remapper, which rewrites call owners - after that pass the
instruction no longer matches `org/bukkit/Material.values()`.

Only that one call is touched. `valueOf`, `getMaterial`, the registries and field
access are left exactly as the plugin compiled them, which
`tools/rewriter_test.sh` asserts explicitly by diffing every call site before and
after.

## Notes on porting decisions

### Shearing was redesigned, not retargeted

NeoForge neutralises the vanilla per-entity shear branches
(`if (false && itemStack.is(Items.SHEARS))`) and routes everything through
`IShearable`. Cardboard's per-mob injections had no call site left, so both
`PlayerShearEntityEvent` and `BlockShearEntityEvent` moved onto the replacement
subsystem. A side effect is that both now fire for modded shearable entities too.

### The world PDC has no datafixer

Cardboard used Fabric's extend-enum to add a null-typed `DataFixTypes.PAPER_NONE`
so plugin data would never be datafixed. That mechanism is Fabric-only, but 26.2
supports the same thing natively: `SavedDataType` has a constructor that leaves
the fix type null, and `SavedDataStorage` skips the datafixer when it is.

### Class shadowing is checked mechanically

`tools/check_class_overlap.py` diffs the built jar against every NeoForge
library. Shipping a second copy of a class NeoForge already provides causes
`LinkageError: loader constraint violation` as soon as it crosses the
TRANSFORMER/app loader boundary — invisible until runtime. The check currently
reports zero overlap; it found 237 shadowed classes when first written,
including `joptsimple`, which the option parser passes across that boundary.

## Tested NeoForge mods

| Mod | Version | Result |
| --- | --- | --- |
| Balm | 26.2.0.5 | Loads and initialises normally alongside the Bukkit layer |
| KumaAPI | 26.2.0.1 | Loads (jarjar'd inside Balm) |
| Shogi / Shogi API | 26.2.0.4 | Loads normally |
| Waystones | 26.2.0.7 | Loads, registers 31 blocks and 48 items, config generated; all 31 blocks reach the Bukkit `Material` registry |

All four run with zero errors alongside plugins. Waystones is the useful one for
cross-ecosystem testing because it registers real blocks, block entities and
items. A larger technology mod with machines and capabilities would exercise the
capability bridge harder; none was available for 26.2 at the time of testing,
since the version is new enough that most content mods have not updated.

## Tested plugins

| Plugin | Result |
| --- | --- |
| `CardboardTest` | Full lifecycle: discovered, loaded, enabled, command registered, disabled on shutdown. 84 of 89 core probes pass. |
| `CardForgeExample` | Full lifecycle, and exercises the integration API against live modded content. |

Both are purpose-built probes rather than third-party plugins. Testing against a
broad set of real-world plugins (EssentialsX, WorldEdit, LuckPerms and similar)
is the obvious next step and has not been done here; the core suite covers the
API surface those plugins depend on, but it is not a substitute for running them.

## Reproducing

```sh
./gradlew jar
python3 tools/check_class_overlap.py build/libs/Cardforge-26.2.jar <server-dir>
cp build/libs/Cardforge-26.2.jar <server-dir>/mods/
tools/cycle_test.sh <server-dir> 3
```

Regression tests:

```sh
# Offline: proves the values() rewrite is correct and nothing else is touched.
tools/rewriter_test.sh <server-dir>/plugins/CardboardTest.jar

# In-server: boots, runs the probes from the console, fails on any FAIL line.
tools/regression_test.sh <server-dir>
```

The in-server test needs Balm, Shogi and Waystones in `<server-dir>/mods/` and
`CardboardTest.jar` in `<server-dir>/plugins/`. `cbtest mods` can also be run by
hand from the server console.
