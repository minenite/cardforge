# CardForge 26.2 compatibility report

Every claim here is backed by something that was actually run. Areas are graded:

- **VERIFIED** — exercised at runtime on a real dedicated server, result observed.
- **PARTIAL** — works, with a stated limit.
- **UNSUPPORTED** — known not to work, with the reason.
- **UNTESTED** — not exercised. Not a claim of either outcome.

A Mixin applying, a clean compile, or a purpose-built probe that happened to
avoid the failing path are **not** evidence. That distinction is not academic:
this project has twice recorded something as working on those grounds and been
wrong both times.

## Architecture

CardForge is a compatibility subsystem on a real NeoForge server, not a
reimplementation of one:

```
NeoForge 26.2 (the real dedicated server)
    -> CardForge (Cardboard's Bukkit/Spigot/Paper implementation, ported)
        -> Bukkit / Spigot / Paper plugins
```

Mods load through NeoForge unchanged. Plugins load through the Bukkit layer.
Where NeoForge replaced a vanilla subsystem, CardForge hooks NeoForge's
replacement rather than restoring the vanilla path.

## VERIFIED

### Automated suite

`tools/regression_test.sh` — **67 probes, 0 failures, 0 server errors** in one
full run.

| Area | Probes |
| --- | --- |
| recipes | 10 |
| boss bars | 10 |
| modded content | 9 |
| entities | 7 |
| `Material.values()` | 4 |
| blocks | 4 |
| registries, PDC | 3 each |
| worlds, scoreboards, projectiles, permissions, ItemStacks, configuration, commands | 2 each |
| world save, scheduler | 1 each |

Plus an isolated `DamageProbe` (`cbtest damage`) and `ItemStackProbe`
(`cbtest item`).

### Server and platform

| Item | Evidence |
| --- | --- |
| Strict Mixin behaviour | `defaultRequire: 1` on all configs, zero failed injections |
| Dedicated server startup | Reaches `Done`, no ERROR/FATAL |
| Repeated lifecycles | Consecutive start/stop cycles, clean shutdown each time |
| Plugin lifecycle | Discovered, loaded, enabled, command registered, disabled on shutdown |
| Class hygiene | `check_class_overlap.py` reports zero classes shadowing a NeoForge library |

### Bukkit/Paper behaviour

| Area | Evidence |
| --- | --- |
| `EntityDamageEvent` | Fires exactly once; **cancelling prevents the damage** and the entity survives; a vanilla explosion arrives as `ENTITY_EXPLOSION` |
| `EntityDeathEvent` | Fires exactly once on lethal damage, after the damage event |
| `setHealth(0)` / `setMaxHealth` | Death sequence runs; max health moves the maximum, not current health |
| `Entity#teleport` (mobs) | Returns true and the world position changes |
| `Entity#remove`, persistence | Removal unresolvable by UUID; `setRemoveWhenFarAway` round-trips |
| `ItemStack.serialize()` | Bukkit's documented map shape; round-trip preserves type, amount, namespaced identity, name, lore, enchantments, PDC — vanilla **and** modded |
| Recipes | Add/lookup/remove for shaped, shapeless, furnace, blasting, smoking, campfire, stonecutting; duplicate keys rejected; 1639 recipes iterate after dynamic additions |
| Boss bars | Create, retrieve by key, retitle, progress, colour, style, visibility, players, enumerate, remove |
| Blocks | Place, read back, `BlockData` round-trip, relative navigation, `breakNaturally` |
| PDC | Item, entity and world containers round-trip |
| Scoreboards, scheduler, permissions, commands, configuration, registries, world save | See suite |

### Cross-ecosystem

| Item | Evidence |
| --- | --- |
| Mods alongside plugins | Balm, KumaAPI, Shogi, Waystones load with the Bukkit layer, zero errors |
| Modded blocks in Bukkit | 32 Waystones blocks registered as `Material`s |
| Modded lookup | By name, by `NamespacedKey`, and through `Registry.ITEM`/`BLOCK`; item-only ids correctly return `null` from `Registry.BLOCK` |
| Modded `ItemStack` | Constructed, serialized, round-tripped |
| Modded block write/read | A plugin places a modded block via Bukkit and reads it back |
| Modded block entities | `Block#getState()` returns a usable `TileState` |
| `Material.values()` | 2204 entries — vanilla plus 50 Waystones materials, no duplicates; other Material calls unchanged |
| Integration API | 8 mods enumerated; modded content resolved by real id; NeoForge item capability resolved as `WorldlyContainerWrapper` |

### Real client (manual playtest)

A NeoForge 26.2.0.52-beta client with Balm, Shogi and Waystones connected to a
server running the same mods plus two plugins. Confirmed in game: handshake and
mod-list check, login, two accounts, PvP, `/tp`, modded GUI (Waystones naming and
warp), block place/break, shearing by hand **and by dispenser**, entity damage
and death, Waystone teleport including Nether-Overworld, and `/cfx caps`
resolving a NeoForge capability.

**That session found six bugs the automated suite could not**, three of which
made the server unusable. Everything a console cannot reach — handshake, client,
player, looking at a block — was where they lived.

## PARTIAL

**`Material.values()` reflection paths.** `values()` itself is handled by a
class-load rewrite to `CardForgeMaterials.values()`. But
`EnumSet.allOf(Material.class)`, `Material.class.getEnumConstants()` and direct
reflection on `$VALUES` read the JDK's cached copy and still return the 1691
vanilla entries. Workarounds: `Material.values()`, `CardForgeMaterials.values()`,
`Material.getMaterial(name)`, or the registries.

**Modded block entities expose no typed API.** They yield a generic `TileState`:
location, type and PDC work; the mod's own contents do not, because no Bukkit
interface describes them. CardForge-native plugins can reach them through
`blockCapability`.

**`BlockShearEntityEvent` drop replacement is all-or-nothing.** NeoForge derives
shear drops by capturing them during `onSheared`, so the list does not exist
until after the entity is sheared, while cancellation must be decided before.
Cancellation is exact; incremental drop editing is not expressible.

**Inventory title changes on the modded menu path.** A plugin changing the title
in `InventoryOpenEvent` has no effect there, because NeoForge reads the title
from the provider after CardForge's hook.

## UNSUPPORTED

Nothing is currently known-broken and unfixed. Every failure found so far has
either been fixed or moved to PARTIAL with a stated limit.

## UNTESTED

Do not read these as working.

- **Real third-party plugins.** LuckPerms, WorldEdit, Essentials-style, protection/claims — none run. Both test plugins are purpose-built probes. The suite covers the API surface such plugins use, which is not the same as running them.
- **Third-party plugins against modded content.**
- **Cross-ecosystem cancellation beyond block placement and shearing.**
- **Client regression after this session's fixes.** The playtest predates the `EntityDamageEvent`, `serialize`, boss bar, recipe and entity-lookup work.
- **Repeated persistence/restart cycles** with plugin, PDC, mod and registry state.
- **Clean-room distributable test** after these fixes.
- **A nontrivial technology mod.** Waystones has blocks and block entities but no machines, so the capability bridge is only lightly exercised. None was available for 26.2 at the time.

## Tested versions

| Mod | Version |
| --- | --- |
| Balm | 26.2.0.5 |
| KumaAPI | 26.2.0.1 |
| Shogi / Shogi API | 26.2.0.4 |
| Waystones | 26.2.0.7 |

| Plugin | Kind |
| --- | --- |
| `CardboardTest` | Purpose-built probe |
| `CardForgeExample` | CardForge-native example |

## Reproducing

```sh
./gradlew jar apiJar dist
python3 tools/check_class_overlap.py build/libs/Cardforge-26.2.jar <server-dir>
python3 tools/audit_overlap.py src/main/java <neoforge-repo>
tools/rewriter_test.sh <plugin.jar>
tools/regression_test.sh <server-dir>
tools/cycle_test.sh <server-dir> 3
```

In-server, from the console: `cbtest auto`, `cbtest damage`, `cbtest item`,
`cfx compare`. `cbtest core <comma,separated,names>` skips probes, which is how
cross-probe interference gets isolated.

## A note on method

Three "bugs" in this project turned out to be artifacts of testing with nobody
online — despawning mobs, chunks not entity-ticking, empty entity lists — and one
looked like cross-test interference but was really a bad spawn position. In every
case the wrong explanation was the plausible one, and only instrumenting each
step settled it. Headless entity and world tests should keep a player connected
or account for that explicitly.

Equally, two real bugs were found in under a minute once measured, after repeated
wrong guesses from reading the code: the `craftDelegate` trace and the boss-bar
stack. Measure before theorising.
