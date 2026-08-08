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

The plugin used is `CardboardTest`, whose `/cbtest mods` runs from the console so
the whole check can execute in an automated boot with no client attached.

## Known limitations

These are real and are listed rather than papered over.

### `Material.values()` does not include modded materials

`Material.values()` returns only the 1691 vanilla materials, even though the
backing `$VALUES` array demonstrably holds all 2204 (verified by reading the
field reflectively at runtime).

The cause is that enum extension writes `$VALUES` through `Unsafe` after class
initialisation, while `values()` compiles to a `getstatic` on a `static final`
field that HotSpot has already constant-folded — `values()` is hot during
registration, so it folds early and then keeps returning the old array.

Attempted fix: the extended array is published to
`org.cardboardpowered.impl.MaterialValues` and `BukkitMaterialMixin` injects into
`values()` to return it. The publish is confirmed to run with the correct
contents, and the injection is confirmed to apply (strict mode would fail the
boot otherwise) — but `values()` still observes the folded constant. Unresolved.

**Impact:** a plugin that *iterates* `Material.values()` will not see modded
content. **Workarounds that do work:** `Material.getMaterial(name)`,
`Material#getKey()`, and `Registry.ITEM` / `Registry.BLOCK` — all verified above.
This affects discovery only, not manipulation.

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

## Reproducing

```sh
./gradlew jar
python3 tools/check_class_overlap.py build/libs/Cardforge-26.2.jar <server-dir>
cp build/libs/Cardforge-26.2.jar <server-dir>/mods/
tools/cycle_test.sh <server-dir> 3
```

For the cross-ecosystem probes, install Balm, Shogi and Waystones into
`<server-dir>/mods/` and `CardboardTest.jar` into `<server-dir>/plugins/`, then
run `cbtest mods` from the server console.
