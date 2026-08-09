# Porting Cardboard from Fabric to NeoForge

Started against Cardboard `ver/26.2` (commit `c8d09f2d`) and NeoForge 26.2.x.

This began as a plan. It is now a record of what the port actually cost, kept in
that order so the estimates can be read against the outcomes. Where a prediction
was wrong, it says so.

## Why it was tractable

Cardboard's Bukkit implementation is almost entirely platform-agnostic. Of 1,532
Java files, only 19 imported `net.fabricmc.*`. The server implementation, plugin
loader, scheduler, command bridge, event system, inventories and permissions are
written against Minecraft classes, not against Fabric.

That held up. This was a platform-adapter swap plus a build-system migration, not
a rewrite. Today no file imports Fabric at all - the five remaining mentions of
`net.fabricmc` are comments explaining what a piece of code replaced.

## The four workstreams, and how they turned out

### 1. Loader API - as estimated

19 files, small. Replaced with a `PlatformAdapter` interface and a
`NeoForgePlatform` implementation, so the Bukkit layer never imports a loader API
directly.

| Fabric | Uses | NeoForge equivalent |
| --- | --- | --- |
| `FabricLoader` | 11 | `ModList` / `FMLPaths` / `FMLLoader` |
| `EventFactory` / `Event` | 8 | NeoForge event bus |
| `ModInitializer` | 2 | `@Mod` + `FMLCommonSetupEvent` |
| `ModContainer` / `ModMetadata` | 3 | `IModInfo` |
| `MappingResolver` | 1 | not needed, both are unobfuscated on 26.2 |
| `KnotServer` / `FabricLauncherBase` | 2 | NeoForge launch handler |
| `fabric.api.screenhandler` | 3 | NeoForge menu registration |

### 2. Mixins - the estimate was right, the risk assessment was not

NeoForge ships Mixin 0.8.7 and MixinExtras 0.5.4, the same stack Cardboard
targets, and the classes carried across largely unchanged. Registration moved
from `bukkitfabric.mixins.json` to configs declared in `neoforge.mods.toml`.
There are now 281 mixin classes across three configs, running under
`defaultRequire: 1` with zero failed injections.

The plan named `@Overwrite` overlap as the main risk. That was the right
category and the wrong shape.

**What was predicted:** friction in the ~96 `@Overwrite`s where NeoForge patches
the same method. In practice there are 40 live `@Overwrite` annotations, 12 of
them in classes NeoForge also patches, and exactly **one** overwrote a method
NeoForge had changed: `handleSetCarriedItem`, which silently discarded
NeoForge's hotbar-switch events. Real, but a much smaller blast radius than
feared.

**What was not predicted, and mattered more:** NeoForge widens signatures and
leaves the old one as a delegate.

```java
// NeoForge keeps the narrow form and moves the body to a wider one
public void hurtAndBreak(int amount, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak) {
    this.hurtAndBreak(amount, level, (LivingEntity) player, onBreak);
}
```

A hook on the narrow signature still applies cleanly, strict mode reports
nothing, and it stops firing for every caller that moved to the wide overload.
This pattern accounted for several defects - `openMenu`, `emptyContents`,
`processDurabilityChange`, `MappedRegistry#register` and `ItemStack#hurtAndBreak`
- and none of them was visible in play. `PlayerItemDamageEvent` was simply dead:
durability still decreased, so nothing looked wrong.

**The lesson worth carrying:** "zero failed injections" means every hook found a
target. It says nothing about whether the target still means what it meant. See
[OVERLAP_AUDIT.md](OVERLAP_AUDIT.md) for the class-by-class audit that followed.

### 3. Access widener to access transformer

`tools/aw2at.py` performs the conversion. Field and class widenings are applied.

Fabric applies access wideners to bytecode at runtime. NeoForge applies access
transformers during a source recompile of Minecraft, so Java's override rules are
enforced. Widening `LivingEntity#getHurtSound` to public while `Witch`,
`WanderingTrader` and every other subclass still declare it protected does not
compile:

```
ERROR getHurtSound(DamageSource) in WanderingTrader cannot override
      getHurtSound(DamageSource) in LivingEntity
```

The 229 method entries are therefore parked in
`accesstransformer-methods.cfg.disabled`, and the handful Cardboard actually
calls are reached with Mixin `@Invoker` accessors instead - 12 files use them
today. That was the predicted cheaper route and it was.

#### Fabric API's transitive access wideners

Not obvious until the port: Cardboard compiles on Fabric partly because Fabric
API ships its own transitive access wideners and Loom applies them to the compile
classpath. `Display#setTransformation`, `#getViewRange` and friends are private in
vanilla and absent from Cardboard's own widener, yet `CraftDisplay` calls them.

NeoForge has no equivalent, so `fabric-transitive-access-wideners-v1` (449 lines)
is converted alongside Cardboard's own widener and merged:

```
Cardboard AW + Fabric API transitive AW -> 1,130 AT entries
468 of 502 method widenings apply; 34 need @Invoker
```

The one entry with no AT form is the extend-enum `DataFixTypes PAPER_NONE`.

### 4. iCommonLib - absorbed

Cardboard depended on iCommonLib in 18 files, and iCommonLib is itself a Fabric
mod. The plan offered two options: port it, or absorb the interfaces and drop the
dependency.

**Absorbed.** 119 source files now live under `me/isaiah/` inside CardForge, with
no build dependency and no separate mod on the server. The reasoning held: the
cross-version abstraction that justifies iCommonLib on Fabric buys nothing
against a single NeoForge target.

One trap the plan did not anticipate. Bundling the classes is not enough:

> Cardboard bundles iCommonLib's classes but on Fabric relies on iCommonLib being
> installed as a separate mod to *register* them.

So CardForge has to register `icommon.mixins.json` itself. Without it,
`IMixinEntity` is never applied to `Entity` and every cast to it throws
`ClassCastException` - which is what broke `launchProjectile`, `Entity#remove`
and keyed boss bars, none of them anywhere near the actual cause.

It is also not purely interface plumbing, as the plan assumed. Of the 31 mixin
files in there, 30 carry live behaviour.

## A fifth workstream the plan missed entirely

Modded content has to reach plugins through `org.bukkit.Material`, which is an
enum. Extending it with `Unsafe` works, but `Material.values()` reads a
`static final $VALUES` that HotSpot has already constant-folded by the time any
plugin runs, so the extra entries stay invisible.

The fix is a class-load bytecode rewrite of plugin `Material.values()` call sites
to a CardForge bridge, preserving the descriptor so precompiled plugin jars need
no changes. Paths that read `Class`'s own enum cache instead -
`getEnumConstants`, `EnumSet`, `EnumMap` - are handled by seeding that cache with
the extended array. Verified at 3,881 materials with 14 mods installed.

None of this exists on Fabric-Cardboard in the same form, and it was the single
largest piece of new engineering in the port.

## Build system

Loom is Fabric-only; the build moved to ModDevGradle. The dependency set
(paper-api, Adventure 5, SpecialSource, Configurate, maven-resolver) carried over
unchanged.

`Libraries.java` downloads paper-api and Adventure at runtime with pinned SHA1s.
That mechanism is platform-independent and was kept.

One NeoForge-specific hazard: shipping a class NeoForge already provides becomes
a `LinkageError` the moment it crosses a classloader.
`tools/check_class_overlap.py` fails the build on that, and it is why the jar
excludes guava, netty, ASM, joptsimple and much of Maven, while deliberately
keeping the Maven resolver that Paper's `LibraryLoader` needs.

## Order it was done in

1. ModDevGradle skeleton compiling an empty mod against NeoForge 26.2
2. Access transformer generated and applied
3. iCommonLib absorbed
4. Platform adapter and entrypoint, enough to reach `CraftServer` construction
5. Mixin registration, then iterating on apply failures
6. Plugin loading, then the command and event bridges
7. The CardboardTest probe plugin

## Where it ended up

A working server. NeoForge 26.2 boots with the Bukkit layer on top, real plugins
(WorldEdit, WorldGuard, LuckPerms, EssentialsX) run against real mods, and modded
content crosses into the Bukkit API in both directions.

What the plan could not have told you is how the remaining defects would behave.
Almost none of them were crashes. Blocks broke, durability decreased, hotbars
switched, ops looked like ops - while the contract underneath was quietly not
being honoured. [COMPATIBILITY.md](COMPATIBILITY.md) records what is verified,
what is partial and what is broken, with the evidence for each.
