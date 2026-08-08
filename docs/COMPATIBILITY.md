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

The plugin used is `CardboardTest`, whose `/cbtest mods` runs from the console so
the whole check can execute in an automated boot with no client attached.

## Known limitations

These are real and are listed rather than papered over. Each is covered by a
probe in `CardboardTest`, so they fail loudly rather than rotting quietly.

### Five core probes still fail

Run `cbtest core` from the console to reproduce. 84 pass, these 5 do not:

| Probe | Symptom | Assessment |
| --- | --- | --- |
| `entities: teleport` | `Entity#teleport` does not move a spawned entity | CardForge gap in the entity teleport path; player teleport is untested here |
| `entities: EntityDamageEvent` | Damage applies, but the event never dispatches | The damage path reaches NMS without firing the Bukkit event |
| `entities: EntityDeathEvent` | Entity dies, but the event never dispatches | Same shape as the damage gap |
| `itemstacks: serialize` | `ItemStack.serialize()` throws NPE, `craftDelegate` is null | Paper's ItemStack delegate is not wired for stacks built plugin-side |
| `recipes: iterator` | `Invalid recipe type: DyeRecipe` | 26.2 added a recipe type Cardboard's converter does not map. Vanilla gap, unrelated to mods |
| `bossbars: keyed` | `CustomBossEvent` cannot be cast to `EntityBridge` | `Bukkit.createBossBar(key, ...)` reuses an entity-oriented bridge that a boss event is not |

None of these are NeoForge-specific: they are Cardboard/26.2 porting gaps that
the modded server merely made visible. They are listed here rather than fixed
because each needs its own investigation, and the suite now fails on them.

### `Material.values()` reflection paths still see the folded array

`Material.values()` itself is handled - see "How `Material.values()` works" below
- but a plugin reaching the same data another way is not. `EnumSet.allOf(Material.class)`,
`Material.class.getEnumConstants()` and direct reflection on `$VALUES` read the
JDK's own cached copy rather than calling `values()`, so they still return the
1691 vanilla entries.

**Workarounds:** call `Material.values()` (rewritten, sees everything),
`CardForgeMaterials.values()` directly, `Material.getMaterial(name)`, or
`Registry.ITEM` / `Registry.BLOCK`.

### Modded block entities expose no typed API

A modded block entity yields `CraftModdedBlockEntity`, a generic `TileState`.
Location, type and the persistent data container work; the mod's own contents are
not typed, because no Bukkit interface describes them.

Before this, the default factory's `Unexpected BlockState` assertion made
`Block#getState()` throw on any modded block entity, which would break every
plugin that scans a region.

### `BlockShearEntityEvent` drop replacement is all-or-nothing

NeoForge derives shear drops by capturing them during `IShearable#onSheared`, so
the drop list does not exist until after the entity has been sheared, while
cancellation must be decided before. The event is therefore fired at
`isShearable` with an empty mutable list, and drops a plugin adds replace the
natural ones. Cancellation is exact; incremental drop editing is not expressible.

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
