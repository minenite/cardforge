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

`tools/regression_test.sh` — **69 probes, 0 failures, 0 server errors** in one
full run, with 5 reported as skipped because they need a connected client.

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

Plus an isolated `DamageProbe` (`cbtest damage`), `ItemStackProbe`
(`cbtest item`), `BreakProbe` (`cbtest break`) and an opt-in `RespawnProbe`
(`cbtest respawn`), which is separate because it has to kill the player for
real.

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
| Modded GUI interaction | Pipez's wrench GUI opens and its config controls work: pipes can be set to Extract and items move. Verified after the click-path and mixin fixes; a menu trace confirms the mod's own `ExtractContainer` reaches `containerMenu` unmodified |
| Server-list ping | `ServerListPingEvent` is iterable; EssentialsX's vanished-player handling runs without error. **Limit:** removing a player from the sample does not yet change the response, since CardForge does not populate a player sample |
| Operator status | `/op` and `/deop` take effect immediately on a connected player, in both directions, and permission resolution follows |
| Permission resolution | Unregistered node is false for a non-op and true for an op; a `FALSE`-default permission is false even for an op; `isPermissionSet` and `getPermission` correct |
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
| Enum access paths | `Material.class.getEnumConstants()` and `EnumSet.allOf(Material.class)` both report **3114**, matching `values()`; a modded material round-trips through `EnumSet` and `EnumMap`. These read Class's own enum cache rather than calling `values()`, so the call-site rewrite never covered them - the cache is now seeded with the extended array. Vanilla lookup through `valueOf`, `getMaterial`, `matchMaterial`, `Registry` and `getKey` unchanged |
| `Material.values()` | **2735 entries** with Biomes O' Plenty installed - 1691 vanilla plus **1044 modded** - no duplicates, other Material calls unchanged. Previously verified at 2204 with Waystones alone, so the enum extension and call-site rewrite hold at roughly 20x the modded content |
| Scale | **531 modded blocks** into the Bukkit registry and **581** into WorldEdit's, with terrain generation altered by TerraBlender. Zero errors, fresh world, no code changes required |
| **NeoForge capabilities** | Verified against Trash Cans, which implements all three handler types: the Item can exposes an item handler only, the Fluid can a fluid handler only, the Energy can an energy handler only, and the Ultimate can **all three** - each resolving through `blockCapability` from a plugin that knows nothing about the mod |
| Integration API | 8 mods enumerated; modded content resolved by real id; NeoForge item capability resolved as `WorldlyContainerWrapper` |

### Real client (manual playtest, current build)

Re-run after this session's damage, serialization, recipe, boss bar and
entity-lookup work. Confirmed with a connected player: mob combat, hand
shearing, **dispenser shearing**, player-dealt sword kills, block place/break,
Waystone GUI and teleport, `/cbtest all` (21 probes), and a clean stop/restart
with inventory, built structures, Waystones and their names, and the mod's
saved data all intact.

An earlier session with a NeoForge 26.2.0.52-beta client with Balm, Shogi and Waystones connected to a
server running the same mods plus two plugins. Confirmed in game: handshake and
mod-list check, login, two accounts, PvP, `/tp`, modded GUI (Waystones naming and
warp), block place/break, shearing by hand **and by dispenser**, entity damage
and death, Waystone teleport including Nether-Overworld, and `/cfx caps`
resolving a NeoForge capability.

**That session found six bugs the automated suite could not**, three of which
made the server unusable. Everything a console cannot reach — handshake, client,
player, looking at a block — was where they lived.

### Third-party plugins

Real plugins, unmodified, from their official releases.

| Plugin | Version | Result |
| --- | --- | --- |
| **WorldEdit** | 7.4.4 | Loads, enables, binds its own NMS adapter (`PaperweightAdapter` for `v26_2`). `//set` places a **modded** block by its real id (`waystones:andesite_waystone`, case-insensitive); setting a region to glass removes a modded block. Zero errors. |
| **WorldGuard** | 7.0.18 | Loads, enables, region data and UUID migration. With `__global__` set to `build: DENY` and `interact: DENY` against a non-op, building, breaking and interaction are all correctly denied - **including on modded blocks**, where right-clicking a Waystone no longer opens the mod's GUI. |
| **EssentialsX** | 2.22.0 | Loads and enables despite declaring `api-version: 1.13`, detects LuckPerms, generates its config and data directories, and correctly refuses commands to a non-operator. |
| **LuckPerms** | 5.5.71 | Loads, enables, H2 storage, Brigadier command registration. `/lp info`, `permission set` and `permission info` all work; nodes persist and read back. |

WorldEdit binding a version-specific NMS adapter is the stronger signal here:
it means CardForge's CraftBukkit internals match what a plugin compiled against
Paper expects, not merely that the public API is present.

**Not a defect:** `//set` on a two-tall block such as a Waystone leaves
independent lower/upper halves rather than a working structure. WorldEdit writes
raw block states without running placement logic and does the same to vanilla
doors and beds on ordinary Paper.

### Cross-ecosystem cancellation

The question is not whether a Bukkit event fires - that is easy and already
covered - but whether cancelling it stops the NeoForge operation. An event that
fires and is then ignored is worse than one that never fires, because the plugin
reports success.

Tested with a protection plugin in miniature (`/cbtest guard`), which cancels
placement, breaking, interaction and damage at HIGHEST priority. 40 cancellations
recorded, zero errors.

| Cancelled event | Outcome |
| --- | --- |
| `BlockPlaceEvent` | Placement prevented, vanilla and modded |
| `BlockBreakEvent` | Breaking prevented, vanilla and modded |
| `PlayerInteractEvent` on a modded block | **The Waystone GUI does not open.** The mod's own interaction handler never runs |
| `EntityDamageEvent` | Damage prevented, including environmental sources - an enderman's `FALL` damage arrived through the bridge with the correct cause and was cancelled |

The interaction case is the important one: it shows a Bukkit plugin can guard
content belonging to a mod that knows nothing about Bukkit.

### Clean-room distributable

Built with `./gradlew dist`, installed into a fresh directory by `install.sh`
(which fetches and runs the official NeoForge installer), then given the four
tested mods and six plugins.

| Run | Result |
| --- | --- |
| First boot | Reaches `Done`, all six plugins enable, **126 probe passes, 0 failures** |
| Restart | Reaches `Done`, **126 passes, 0 failures**, clean shutdown both times |
| CardForge errors | **0** across both runs |

Plugin data (`Essentials/`, `LuckPerms/`, `CardboardTest/`) and mod saved data
(`waystones.dat`) are created on the first run and survive the restart.

The only error logged is a Mojang API fetch failure for the Yggdrasil public
key, which is network reachability and not CardForge.

### Harness coverage

`regression_test.sh` drives the console, and `probeNms`, `probeItemMeta` and
`openGui` used to require a player, so an entire probe category never ran in
automation - and an absent probe reads exactly like a passing one. Most of it
never needed a player and now runs headlessly; the four checks that genuinely
need a connected client report `[SKIP]` with the reason.

The harness distinguishes the two claims: exit 0 means everything ran and
passed, exit 2 means everything that ran passed but some probes were skipped.
"Nothing failed" can no longer be read as "everything ran".

### Defects found by the overlap audit, fixed and covered

Four hooks were bound to NeoForge code that had moved underneath them. All four
are fixed, and **none was visible in play** - the visible behaviour was correct
while the contract underneath was not. Full class-by-class working in
[OVERLAP_AUDIT.md](OVERLAP_AUDIT.md).

| Defect | Symptom before the fix | Now covered by |
| --- | --- | --- |
| `destroyBlock` fired a second break event in parallel with NeoForge's | Plugin cancellation never reached mods, and mod cancellation never reached plugins | `cbtest break`: event fires exactly once, cancellation composes, pre-cancelled breaks still notify |
| `ItemStack#hurtAndBreak` hooked a delegate | `PlayerItemDamageEvent` never fired; durability still decreased, so nothing looked wrong | `cbtest break`: event fires from the real tool-damage path |
| `MappedRegistry#register` hooked a delegate | Registrations made through the wide overload were missed by Paper's registry API | reasoning recorded; no probe |
| `handleSetCarriedItem` overwrote a patched method | NeoForge's hotbar-switch events silently discarded | boot-time strict mode only |

### Defects found by playing, fixed and covered

| Defect | Symptom | Now covered by |
| --- | --- | --- |
| Respawn placement refused by Paper's dead-entity guard | Client stuck on "loading terrain" forever while its body stayed in the world | `cbtest respawn` |
| `CraftPlayer` not rebound across respawn | A `Player` reference held across death pointed at the corpse | `cbtest respawn` |
| `Player#getHealth()` returned a cached constant | Every player read as 20.0 regardless of damage, healing or death | `health` probe in the player sweep |
| `CraftHumanEntity.op` never seeded from the op list | Every op-default plugin permission denied after a restart; EssentialsX `/i` and WorldGuard bypass both failed | `cbtest perm` asserts `isOp()` matches the op list |
| Unmapped mod entities threw from inside the world tick | First natural spawn of any unrecognised mod entity crashed the server | entity-wrapping probe over every loaded entity |
| `/reload` handed closed jar handles to new classloaders | Every plugin failed to load and the server came back up with none, still running | two consecutive reloads verified by hand |
| `CompoundContainer#getMaxStackSize` merged over vanilla's | Reported 64 regardless of what either half allowed | verified in exported bytecode |

### Scale

With 14 mods installed including Mekanism (all four modules), `Material.values()`
reports **3,881 entries with no duplicates**, and `getEnumConstants`,
`EnumSet.allOf` and `EnumMap` all agree with it. Vanilla lookup through
`valueOf`, `getMaterial`, `matchMaterial`, `Registry` and `getKey` is unchanged.
Boot time did not move (9.1s against 9.5s without Mekanism).

### Lifecycle

- **Restart torture:** 8 consecutive stop/start cycles, each requiring `Done`,
  plugins enabling *and* disabling, a clean `Stopping server`, and zero
  ERROR/FATAL lines. All 8 passed.
- **`/reload`:** two consecutive reloads, all six plugins back each time, probe
  suite green afterwards. Discouraged upstream regardless; "works" here means it
  no longer destroys the server.

### Two players

Two accounts connected simultaneously: shared chest inventories, `/op` and
`/deop` taking effect live on a connected player, WorldGuard denying a non-op,
and PvP once the operator's own `__global__` deny flags were cleared.

## PARTIAL

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

### EssentialsX `/itemdb` errors on modded items

Holding a modded item and running `/itemdb` prints the correct
`ITEM: WAYSTONES_ANDESITE_WAYSTONE` line and then an error:
`Cannot invoke "Object.toString()" because "each" is null`.

**This is an EssentialsX limitation, not a CardForge fault.**
`FlatItemDb#name(ItemStack)` resolves an item against EssentialsX's own `items`
map, which is built from a bundled vanilla-only item database. A modded item has
no row, so `name()` returns null, the null is added to `nameList()`, and
EssentialsX's list joiner calls `toString()` on it. The same happens on any
modded server running EssentialsX.

Note what works: the `ITEM:` line comes from `Material.toString()` and is
correct, so CardForge handed EssentialsX a valid modded material. Only the
alias lookup has no entry.

**Possible enhancement, not a fix.** Cardboard already injects modded content
into WorldEdit's registry at startup; the same could be done for EssentialsX's
item database. That would be a new feature and is not currently implemented.

### Intermittent NoClassDefFoundError on core Bukkit classes (explained, fixed)

Previously recorded here as unexplained. It was not a CardForge fault: it was
the deploy procedure. Copying a new jar over `mods/Cardforge-26.2.jar` while the
server ran corrupted the running JVM's view of the archive. `cp` rewrites in
place and keeps the inode, but the JVM holds the zip open with the entry offsets
it read at startup, so afterwards every cached offset points into unrelated
bytes:

```
NoClassDefFoundError: org/bukkit/craftbukkit/event/CraftEventFactory
Caused by: java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)
```

Only classes not yet loaded fail, which is why it looked random, and why the
classes were provably present in the jar - the file on disk was fine, the JVM's
index of it was not.

Reproduced deliberately rather than inferred: boot clean with 94 probes passing,
copy a jar with shifted entry offsets over it, and the failure appears on the
next cold class load. A first attempt did not reproduce it because `zip` had
appended the padding after the existing entries, leaving their offsets
untouched - which confirms the mechanism from the other side.

`tools/deploy.sh` now refuses to install while a server is running from the
target directory, and otherwise installs atomically via rename. It identifies a
running server by process cwd rather than a command-line pattern, since several
things on a dev box match "neoforge", including the Minecraft client.

## UNSUPPORTED

### Clicking in a modded GUI (fixed)

Was recorded here as unfixed: the GUI opened and its buttons did nothing, so a
Pipez pipe could never be set to Extract. It now works, verified in game.

Two changes landed close together and the credit cannot be split cleanly between
them: the `InventoryClickEvent` mixin returned early for non-slot widgets while
leaving `doCl` false, and a `@Redirect` drops `clicked()` when `doCl` is false -
so "skip the Bukkit event" silently meant "drop the click". The other was the
`PrepareRamNearestTarget` retarget below. The first is the plausible cause; that
is not the same as knowing.

### Strict mode catches broken injections late, not at boot

Mixin resolves a config's targets when the target class is **first loaded**, not
at startup. A live injection that can never bind therefore passes boot silently
and takes the server down whenever something first needs that class.

This happened: `PrepareRamNearestTargetMixin` targeted `method_36270`, a Fabric
intermediary name left over from Cardboard's origin that means nothing under
Mojang mappings. The server ran for hours and then crashed during chunk
generation, when a ram behaviour first loaded. Retargeted to `lambda$start$2`,
the equivalent under Mojang names.

`tools/audit_overlap.py` now reports live injections targeting `method_NNNNN`
so this class of latent crash is caught statically. It strips comments first,
since most surviving intermediary names here are inside disabled code.



Nothing is currently known-broken and unfixed. Every failure found so far has
either been fixed or moved to PARTIAL with a stated limit.

## UNTESTED

Do not read these as working.

- **Sustained uptime.** The server has never been left running for hours. This is
the largest remaining gap, and the one that matters most: a broken hook can pass
boot and fire when a class first loads, which is exactly how the
`PrepareRamNearestTarget` crash behaved.
- **Load.** Two players idling is not load. Nothing has been measured under
concurrent activity, and there are no TPS or memory figures at all.
- **Plugin messaging, database-backed plugins, dimension travel combined with
death.**
- **A protection or claims plugin beyond WorldGuard.**

## NOT CARDFORGE

Recorded because each cost real time to attribute, and the attribution is the
useful part.

- **Modded fluids you cannot walk into.** A player floats on the surface, frozen.
Reproduced with CardForge removed from `mods/` entirely, and again in
single-player with no server at all. Vanilla water and lava are unaffected, and
so is at least one modded fluid. Zero movement rejections were logged across
every run, so the server never refused the movement - it is client-side. Belongs
to the mod stack.
- **EssentialsX `/itemdb` and `/give` on modded items.** Its bundled item
database is vanilla-only.
- **WorldEdit `//set` producing half of a two-block structure.** Raw block
states; identical on Paper.

## Tested versions

| Mod | Version |
| --- | --- |
| Balm | 26.2.0.5 |
| KumaAPI | 26.2.0.1 |
| Shogi / Shogi API | 26.2.0.4 |
| Biomes O' Plenty | 26.2.0.0.26 | Large content mod that alters terrain generation |
| TerraBlender | 26.2.0.0.2 | Biome/worldgen framework |
| GlitchCore | 26.2.0.0.0 | BoP dependency |
| Waystones | 26.2.0.7 |
| Trash Cans | 1.0.18 | Item, fluid and energy capabilities |
| Easy Villagers | 1.1.43 |
| Energized Power | 3.0.0-rc.1 | Tech mod, machines and energy |
| Pipez | 1.2.31 | Item/fluid/energy transport |
| SuperMartijn642 config + core lib | 1.1.8 / 1.1.23a |
| **Mekanism** (+ Additions, Generators, Tools) | 10.8.0 built from source | Large tech mod, multiblocks, ~767 added materials |

| Plugin | Version | Kind |
| --- | --- | --- |
| WorldEdit | 7.4.4 | Binds `PaperweightAdapter` for `v26_2` |
| WorldGuard | 7.0.18 | Region protection, enforced against modded blocks |
| LuckPerms | 5.5.71 | Permissions |
| EssentialsX | 2.22.0 | General-purpose, loads with `api-version: 1.13` |
| `CardboardTest` | - | Purpose-built probe |
| `CardForgeExample` | - | CardForge-native example |

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

Later sessions produced sharper versions of the same lesson.

**Isolation settles disagreements faster than argument.** Twice a fault was
attributed by removing CardForge from `mods/` and re-testing: once it proved
CardForge *was* at fault when the reasoning said otherwise, and once it cleared
CardForge of a fluid bug that reproduced in single-player with no server at all.

**Exported bytecode beats inference.** `-Dmixin.debug.export=true` writes the
transformed classes out. It answered in one step a question that source reading
could not - whether a merged method had replaced its target - and it should be
the first tool reached for, not the last.

**Every static check here has been wrong at least once.** The audit tooling gave
confident false clears four separate times: a regex that broke on JVM
descriptors, delegate detection that only matched value-returning delegates,
annotations counted inside commented-out code, and enclosing declarations read
only from hunk headers. Each fix changed the numbers. Treat the tools as a way to
narrow the queue, never as the verdict.

**"No injection annotation" is not a safety argument.** Mixin merges plain
methods and interface implementations with no annotation at all. 41 classes
cleared on that basis turned out to be live code, and one of them supplied an
entire method implementation the registry API depends on.
