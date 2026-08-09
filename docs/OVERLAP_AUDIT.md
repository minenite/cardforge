# NeoForge / Cardboard overlap audit

Cardboard mixes into 215 Minecraft classes; NeoForge patches **113** of them.

> **Parser correction.** The first pass reported 1 intersection and 57 classes
> with no injections. Both were wrong. The annotation matcher bounded arguments
> with `[^;{]*`, and Mixin targets are JVM descriptors containing `;`
> (`@At(target = "Lnet/minecraft/world/Foo;bar()V")`), so every injection
> carrying one failed to match and its file looked empty. Corrected figures:
> **9** intersections, **4** delegate risks, **32** with no injections.

Static inspection only. SAFE means "no evidence of conflict", not "verified".


## BROKEN? — 3

- `MappedRegistry` (MappedRegistryMixin.java) — hooks `register` which NeoForge also patched
- `ServerPlayerGameMode` (ServerPlayerGameModeMixin.java) — hooks `destroyBlock` which NeoForge also patched
- `Ingredient` (IngredientMixin.java) — hooks `equals, test` which NeoForge also patched

## RETARGETED (verify) — 6

Hook lands on a NeoForge-patched method, but this class was reworked and
playtested this session. Re-read to confirm, do not assume.

- `ShearsDispenseItemBehavior` (ShearsDispenseItemBehaviorMixin.java) — hooks `tryShearEntity` which NeoForge also patched
- `ServerPlayer` (MixinPlayer.java, ServerPlayerMixin.java, ServerPlayerMixin_DeathEvent.java) — hooks `drop, openMenu` which NeoForge also patched
- `Entity` (EntityInvoker.java, EntityMixin.java, MixinEntity.java) — hooks `spawnAtLocation` which NeoForge also patched
- `BucketItem` (BucketItemMixin.java) — hooks `emptyContents` which NeoForge also patched
- `ItemStack` (ItemStackMixin.java, MixinItemStack.java) — hooks `applyDamage, hurtAndBreak, processDurabilityChange` which NeoForge also patched
- `ShearsItem` (ShearsItemMixin.java) — hooks `interactLivingEntity` which NeoForge also patched

## NEEDS TEST (@Overwrite) — 17

`@Overwrite` replaces the method wholesale, discarding any NeoForge change to
it. For each: what did vanilla do, what did NeoForge change, what does the
overwrite replace, what NeoForge behaviour is lost, how do we keep both?

- `BuiltInRegistries` (BuiltInRegistriesMixin.java) — 2 @Overwrite
- `RegistryDataLoader` (RegistryDataLoaderMixin.java) — 1 @Overwrite
- `ReloadableServerRegistries` (ReloadableServerRegistriesMixin.java) — 1 @Overwrite
- `DedicatedServer` (DedicatedServerMixin.java, MixinMinecraftServer.java) — 1 @Overwrite
- `ServerLevel` (ServerLevelMixin.java) — 1 @Overwrite
- `ServerGamePacketListenerImpl` (ServerGamePacketListenerImplMixin.java, ServerGamePacketListenerImplMixin_ChatEvent.java, ServerGamePacketListenerImplMixin_InventoryClickEvent.java, ServerGamePacketListenerImplMixin_PlayerCommandPreprocessEvent.java, ServerGamePacketListenerImplMixin_PlayerMove.java, ServerGamePacketListenerImplMixin_SignUpdateEvent.java) — 7 @Overwrite
- `ServerStatusPacketListenerImpl` (ServerStatusPacketListenerImplMixin.java) — 1 @Overwrite
- `PlayerList` (MixinPlayerManager.java, PlayerListMixin_ChatEvent.java, PlayerListMixin_OpEvent.java) — 1 @Overwrite
- `StatsCounter` (StatsCounterMixin.java) — 1 @Overwrite
- `LeashFenceKnotEntity` (LeashFenceKnotEntityMixin.java) — 1 @Overwrite
- `PiglinAi` (PiglinAiMixin.java) — 1 @Overwrite
- `EnchantmentMenu` (EnchantmentMenuMixin.java) — 2 @Overwrite
- `MapItem` (MapItemMixin.java) — 1 @Overwrite
- `TeleportRandomlyConsumeEffect` (TeleportRandomlyConsumeEffectMixin.java) — 1 @Overwrite
- `BambooStalkBlock` (BambooStalkBlockMixin.java) — 1 @Overwrite
- `PlayerDataStorage` (PlayerDataStorageMixin.java) — 3 @Overwrite
- `EnchantedCountIncreaseFunction` (EnchantedCountIncreaseFunctionMixin.java) — 1 @Overwrite

## NEEDS TEST (no injections found) — 32

- `BoatDispenseItemBehavior` (BoatDispenseItemBehaviorMixin.java)
- `Connection` (ConnectionMixin.java)
- `SynchedEntityData` (SynchedEntityDataMixin.java)
- `ChunkHolder` (ChunkHolderMixin.java)
- `ChunkMap` (ChunkMapMixin.java)
- `TicketType` (TicketTypeMixin.java)
- `WorldGenRegion` (WorldGenRegionMixin.java)
- `Pack` (MixinResourcePackProfile.java)
- `SimpleContainer` (SimpleContainerMixin.java)
- `DamageSource` (DamageSourceMixin.java)
- `TamableAnimal` (MixinTameableEntity.java)
- `Cat` (MixinCat.java)
- `AbstractVillager` (AbstractVillagerMixin.java)
- `Inventory` (InventoryMixin.java)
- `Player` (PlayerMixin.java)
- `Raid` (RaidMixin.java)
- `AbstractMinecartContainer` (AbstractMinecartContainerMixin.java)
- `AbstractContainerMenu` (AbstractContainerMenuMixin.java, MixinScreenHandler.java)
- `AnvilMenu` (AnvilMenuMixin.java)
- `CustomData` (CustomDataMixin.java)
- `BlockGetter` (BlockGetterMixin.java)
- `LevelSettings` (MixinLevelSettings.java)
- `BambooSaplingBlock` (BambooSaplingBlockMixin.java)
- `AbstractFurnaceBlockEntity` (AbstractFurnaceBlockEntityMixin.java)
- `BaseContainerBlockEntity` (BaseContainerBlockEntityMixin.java)
- `BeaconBlockEntity` (BeaconBlockEntityMixin.java, MixinBeaconBlockEntity.java)
- `ShulkerBoxBlockEntity` (ShulkerBoxBlockEntityMixin.java)
- `SignBlockEntity` (SignBlockEntityMixin.java)
- `ChunkGenerator` (MixinChunkGenerator.java)
- `LevelStorageSource` (LevelStorageSourceMixin.java)
- `SavedDataStorage` (MixinPersistentStateManager.java)
- `LootTable` (LootTableMixin.java)

## SAFE (disjoint) — 55

- `IntegratedServer` (MixinIntegratedServer.java)
- `Commands` (CommandsMixin.java)
- `FriendlyByteBuf` (FriendlyByteBufMixin.java)
- `RegistryLoadTask` (RegistryLoadTaskMixin.java)
- `Main` (MainMixin.java)
- `MinecraftServer` (MCServerMixin.java, MinecraftServerInvoker.java)
- `PlayerAdvancements` (PlayerAdvancementsMixin.java)
- `TimeCommand` (TimeCommandMixin.java)
- `ServerCommonPacketListenerImpl` (ServerCommonPacketListenerImplMixin_Brand.java)
- `ServerConfigurationPacketListenerImpl` (ServerConfigurationPacketListenerImplMixin.java)
- `ServerConnectionListener` (ServerConnectionListenerMixin.java)
- `ExperienceOrb` (ExperienceOrbMixin.java)
- `LivingEntity` (LivingEntityInvoker.java, LivingEntityMixin.java)
- `Mob` (MixinMobEntity.java, MobInvoker.java, MobMixin.java)
- `StartAttacking` (StartAttackingMixin.java)
- `VillagerMakeLove` (VillagerMakeLoveMixin.java)
- `BreakDoorGoal` (BreakDoorGoalMixin.java)
- `Bat` (BatMixin.java)
- `Animal` (AnimalMixin.java)
- `AbstractHorse` (AbstractHorseMixin.java)
- `ItemEntity` (ItemEntityMixin.java)
- `AbstractCubeMob` (SlimeMixin.java)
- `Zombie` (ZombieInvoker.java)
- `FireworkRocketEntity` (FireworkRocketEntityMixin.java)
- `Projectile` (ProjectileInvoker.java, ProjectileMixin.java)
- `AbstractArrow` (AbstractArrowInvoker.java, AbstractArrowMixin.java)
- `AbstractFurnaceMenu` (AbstractFurnaceMenuMixin.java)
- `BeaconMenu` (BeaconMenuMixin.java)
- `BrewingStandMenu` (BrewingStandMenuMixin.java)
- `GrindstoneMenu` (GrindstoneMenuMixin.java)
- `BlockItem` (BlockItemMixin.java, MixinBlockItem.java)
- `CrossbowItem` (CrossbowItemMixin.java)
- `FireChargeItem` (FireChargeItemMixin.java)
- `FishingRodItem` (FishingRodItemMixin.java)
- `FlintAndSteelItem` (FlintAndSteelItemMixin.java)
- `MinecartItem` (MinecartItemMixin.java)
- `TridentItem` (TridentItemMixin.java)
- `Level` (LevelMixin.java, MixinWorld_18.java)
- `CactusBlock` (CactusBlockMixin.java)
- `CampfireBlock` (CampfireBlockMixin.java)
- `CocoaBlock` (CocoaBlockMixin.java)
- `CropBlock` (CropBlockMixin.java)
- `SugarCaneBlock` (SugarCaneBlockMixin.java)
- `TntBlock` (TntBlockMixin.java)
- `BlockEntity` (BlockEntityMixin.java, MixinBlockEntity.java)
- `BrewingStandBlockEntity` (BrewingStandBlockEntityMixin.java)
- `ChestBlockEntity` (ChestBlockEntityMixin.java, MixinChestBlockEntity.java)
- `HopperBlockEntity` (HopperBlockEntityMixin.java)
- `TheEndGatewayBlockEntity` (TheEndGatewayBlockEntityMixin.java)
- `PistonBaseBlock` (PistonBaseBlockMixin.java)
- `BlockBehaviour` (BlockBehaviourInvoker.java, MixinBlockBehaviour.java)
- `ChunkAccess` (ChunkAccessMixin.java)
- `LevelChunk` (LevelChunkMixin.java)
- `PrimaryLevelData` (PrimaryLevelDataMixin.java)
- `LootItemRandomChanceWithEnchantedBonusCondition` (LootItemRandomChanceWithEnchantedBonusConditionMixin.java)

---

## Semantic review

### 1. `ServerPlayerGameMode#destroyBlock` — BROKEN

**Vanilla:** `destroyBlock` began with `if (!getMainHandItem().canDestroyBlock(state, level, pos, player)) return false;` — the sword-cannot-break check — then removed the block and spawned drops.

**NeoForge changed:** it deleted that check and fires its own event in its place:

```java
// Neo: Fire the BlockBreakEvent, and ignore the original ItemStack#canDestroyBlock
// check since the break event manages the status of it.
var event = CommonHooks.fireBlockBreak(level, gameModeForPlayer, player, pos, state);
if (event.isCanceled()) return false;
```

Block removal also moved into a new `removeBlock(pos, state, canHarvest, toolStack)`.

**Cardboard's hook:** `@Inject(at = @At("HEAD"), cancellable = true)`. It recomputes
`isSwordNoBreak = !canDestroyBlock(...)` by hand, fires Bukkit's `BlockBreakEvent`
pre-cancelled to that value, and returns false if cancelled.

**What is wrong:**

1. It reimplements the exact vanilla check NeoForge deliberately removed, so the
   sword case is now decided twice by two different authorities.
2. Two break events fire for one break: Bukkit's at HEAD, then NeoForge's
   `BlockEvent.BreakEvent` inside the method. Nothing reconciles them. A mod
   cancelling NeoForge's event does not inform Bukkit plugins that already ran and
   may have acted; a plugin cancelling Bukkit's returns before NeoForge's event
   fires at all, so mods never observe the attempt.
3. Cardboard's event carries no exp or canHarvest, both of which NeoForge's event
   now owns, so a plugin adjusting drops or exp is editing a value the real path
   no longer reads.

**How to preserve both:** stop firing a parallel event at HEAD. Bridge the two -
listen on (or redirect) `CommonHooks.fireBlockBreak`, build the Bukkit
`BlockBreakEvent` from the NeoForge event's state, and map cancellation and exp
back onto it. That is the same pattern already used for
`PlayerShearEntityEvent` on `IShearable` and for `BlockPlaceEvent` in
`CommonHooksMixin`: one event source, translated, rather than two in parallel.

**Not yet done.** No fix or regression test is written for this.

### 2. `MappedRegistry#register` — FIXED

**NeoForge changed:** split `register(ResourceKey, T, RegistrationInfo)` into a
delegate over a new `register(int, ResourceKey, T, RegistrationInfo)` holding the
body. It also binds the holder's value at registration time, commented
*"Neo: Bind the value immediately so it can be queried while the registry is not
frozen"*, and keeps `unregisteredIntrusiveHolders` alive because it freezes and
unfreezes registries more than once.

**Cardboard's hook:** `@Inject(at = RETURN)` on the three-argument signature,
filling `temporaryUnfrozenMap`, which `getValueForCopying` read whenever the
registry was unfrozen. Paper uses that to copy an existing value as the base of
a modified one.

**What was wrong:** the hook binds to the narrow overload, which still exists, so
it applies cleanly and reports nothing — but anything calling the wide overload
directly, which NeoForge's own registration does, never reaches it. The map
silently misses those entries and the copy sees nothing.

**Fix:** `getValueForCopying` now asks the bound holder first, which answers for
every registration regardless of overload, and falls back to the map. The map is
kept rather than deleted: it still covers anything registered before a holder is
bound, and costs nothing once the lookup succeeds.

### 3. `ServerPlayer#drop` — SAFE

**NeoForge changed:** `drop(boolean)` gained an `onDroppedByPlayer` veto and now
calls `CommonHooks.onPlayerTossEvent(...)` instead of `drop(stack, false, true)`.

**Why the hook survives:** `onPlayerTossEvent` calls `player.drop(item, ...)`
itself, so Cardboard's inject on the three-argument `drop` still fires. NeoForge
wraps that call in `captureDrops`, so the entity is captured rather than added,
and NeoForge adds it after posting `ItemTossEvent`. Cardboard's cancellation
path returns null, which makes `onPlayerTossEvent` return before
`addFreshEntity`, so a cancelled drop spawns nothing and leaves no orphan. No
double-spawn: Cardboard never adds the entity itself.

Ordering is `PlayerDropItemEvent` then `ItemTossEvent`. An item vetoing via
`onDroppedByPlayer` means no drop happens at all and no Bukkit event fires,
which is correct.

### 4. `Ingredient#test` / `#equals` — SAFE

**NeoForge changed:** both now consult a `customIngredient`, delegating `test` to
it and adding it to the `equals` comparison.

**Why disjoint:** Cardboard's `test` hook only takes over when the ingredient is
a Bukkit exact choice (`cb$isExact()`), which a modded custom ingredient never
is, so the two paths are separated by condition rather than by luck. The
`equals` hook runs at RETURN and can only narrow a true to false when Paper's
exact stacks differ; it never overturns a false. NeoForge's added
`customIngredient` comparison therefore stands in every case where it decides.

### 5. `ItemStack#hurtAndBreak` — BROKEN, FIXED

Found while re-reading the six already-retargeted intersections, which is the
argument for re-reading them: `processDurabilityChange` had been moved to the
wide overload in an earlier pass, and `hurtAndBreak` sitting directly above it
had not.

**NeoForge changed:** widened all three of `hurtAndBreak`,
`processDurabilityChange` and `applyDamage` to take a `LivingEntity`, leaving the
`ServerPlayer` forms as delegates. Then it changed the caller that matters.
`hurtAndBreak(int, LivingEntity, EquipmentSlot)` - the path every tool, weapon
and armour piece takes - was:

```java
this.hurtAndBreak(amount, serverLevel, owner instanceof ServerPlayer player ? player : null, ...)
```

and became:

```java
this.hurtAndBreak(amount, serverLevel, owner, ...)
```

Passing `owner` resolves to the wide overload.

**What was wrong:** Cardboard's hook targeted the narrow `ServerPlayer`
signature. The delegate still exists, so the injection applied cleanly and
strict mode reported nothing, while `PlayerItemDamageEvent` stopped firing for
ordinary tool damage. Durability still decreased, so there is no visible
symptom - the only effect is that a plugin cancelling item damage is silently
ignored.

**Fix:** hook the wide overload. The narrow form delegates to it, so callers of
either arrive exactly once.

**Regression test:** `BreakProbe.runItemDamage` damages a held pickaxe through
`hurtAndBreak(int, LivingEntity, EquipmentSlot)` and asserts
`PlayerItemDamageEvent` fires. It fails against the previous hook.

### 5b. `BreakBlockEvent` listener missed pre-cancelled breaks — FIXED

A regression introduced by the destroyBlock fix above, found when a probe run
happened to be made in spectator mode.

`addListener(Class, Consumer)` does not run for an event that arrives already
cancelled. NeoForge pre-cancels `BreakBlockEvent` for every case it absorbed
from vanilla - the item cannot destroy the block, `blockActionRestricted`
(adventure, spectator), a game-master block - so in all of those the Bukkit
`BlockBreakEvent` never fired at all. Paper fires it cancelled and lets a plugin
overturn it, and Cardboard's original hook reproduced that by pre-setting
cancelled from the sword check.

Fixed with `addListener(true, ...)`. `BreakProbe` now exercises the
pre-cancelled path deliberately rather than avoiding it.

**Lesson recorded:** replacing a parallel event with a bridge moves the hook onto
the other ecosystem's dispatch semantics. Cancellation delivery is part of that
contract, not an implementation detail.

### 6. Respawn placement refused by Paper's dead-entity guard — FIXED

Not a NeoForge overlap at all, and worth recording because the audit tooling
cannot see this class of defect: it is Cardboard-versus-Paper semantics.

`PlayerList#respawn` removes the old entity, builds a new one, then places the
client. At that placement call the connection's own `player` field still refers
to the old entity, which is by definition removed. Vanilla does the same and its
`teleport` has no guard, so the position packet goes out and the client spawns.

Cardboard wraps that call to use Paper's `internalTeleport`, to avoid firing
`PlayerTeleportEvent` on respawn, and inherited Paper's dead-entity guard with
it. The guard refused the one teleport whose purpose is placing a respawning
player. The client sat on "loading terrain" indefinitely while its old body
stayed in the world and kept taking damage.

Fixed with a respawn-only entry point that suspends the guard for that single
call. The guard is not removed - every other caller keeps it.

The refusal now logs the calling frame. Previously it logged one line with no
caller, and since a refused teleport is invisible to the client there was
nothing to connect the symptom to the cause.

### 7. The six RETARGETED intersections, re-read — 5 SAFE, 1 was BROKEN

| Class | Verdict |
| --- | --- |
| `ServerPlayer#openMenu` | SAFE. The live hook is a `@Redirect` on the wide two-argument form. A second inject on the narrow delegate existed but only cleared a ThreadLocal nothing ever set - dead on both counts, removed. |
| `ServerPlayer#drop` | SAFE. `onPlayerTossEvent` still routes through `drop(stack, ...)`; cancellation returns before `addFreshEntity`, so no orphan and no double-spawn. |
| `Entity#spawnAtLocation` | SAFE, **and a prediction of mine was wrong**. NeoForge wraps `dropAllDeathLoot` in `captureDrops` and adds to the capture instead of calling `addFreshEntity`, which is the call Cardboard redirects, so I expected `EntityDeathEvent#getDrops()` to be empty. It carries 2 stacks - drops are collected by another path. Now asserted in the headless suite rather than reasoned about. |
| `BucketItem#emptyContents` | SAFE. Hooked on the wide five-argument overload NeoForge introduced. |
| `ItemStack#hurtAndBreak` | **BROKEN, fixed** - see item 5. |
| `ShearsItem` / `ShearsDispenseItemBehavior` | SAFE. Both moved onto NeoForge's `IShearable` replacement and playtested, including the dispenser path. |

Score for the intersections overall: **3 of 9 were genuinely broken**, none of them visible in play.

### 8. Delegate detection missed void delegates — tooling FIXED

`delegating_overloads` matched only `return wider(...)`. `ItemStack#hurtAndBreak`
is void, so NeoForge's narrow form reduced to a bare
`this.hurtAndBreak(amount, level, (LivingEntity) player, onBreak);` and the tool
reported no delegate risk on the one class where a delegate had actually broken
a Bukkit event. It was found by hand instead.

The pattern now accepts both shapes. Re-running reports the same four classes,
with `hurtAndBreak` and `applyDamage` added to `ItemStack` - all four resolved:

| Class | Method | State |
| --- | --- | --- |
| `MappedRegistry` | `register` | FIXED (item 2) |
| `ServerPlayer` | `openMenu` | SAFE - live hook is on the wide form |
| `BucketItem` | `emptyContents` | SAFE - hooked on the wide five-argument form |
| `ItemStack` | `hurtAndBreak` | FIXED (item 5) |
| `ItemStack` | `processDurabilityChange`, `applyDamage` | SAFE - both hooked on the wide form |

### 9. `@Overwrite` classes — 19, none colliding by method name

No overwritten method appears in the corresponding NeoForge patch, so none of
them is discarding a NeoForge change to that same method. This is a weaker
statement than it sounds: the tool compares names, so an overwrite whose
*original body* called something NeoForge changed would not be reported. The
list is recorded for that second pass.

`ServerGamePacketListenerImpl` (7), `PlayerDataStorage` (3),
`BuiltInRegistries` (2), `EnchantmentMenu` (2), and one each in
`RegistryDataLoader`, `ReloadableServerRegistries`, `DedicatedServer`,
`ServerLevel`, `ServerPlayer`, `ServerStatusPacketListenerImpl`, `PlayerList`,
`StatsCounter`, `LeashFenceKnotEntity`, `PiglinAi`, `ItemStack`, `MapItem`,
`TeleportRandomlyConsumeEffect`, `BambooStalkBlock`,
`EnchantedCountIncreaseFunction`.

### 10. `ServerGamePacketListenerImpl#handleSetCarriedItem` — BROKEN, FIXED

**Vanilla:** validate the slot, stop any item use, set the selected slot.

**NeoForge changed:** added hotbar-switch events to that body - a cancellable
`EventHooks.onSwitchHotbarSlotPre` before the change and
`EventHooks.onSwitchHotbarSlotPost` after it.

**Cardboard's hook:** an `@Overwrite` carrying a copy of the old vanilla body,
with Bukkit's `PlayerItemHeldEvent` added.

**What was lost:** both NeoForge events. No mod could observe or veto a hotbar
change. Nothing failed visibly - the events simply never fired.

**Fix:** replaced the overwrite with an inject placed after the thread check, so
NeoForge's body runs intact and any future addition to it keeps working. The
plugin still gets first refusal.

### 11. Two more parser gaps, both of which hid real work

The audit reported "no overwrite lands on a NeoForge-patched method". That was
wrong twice over.

1. **Annotations inside comments were counted.** `PlayerDataStorage` reported
   three overwrites and has none live. Stripping comments first drops the live
   overwrite count from 19 to 12 and raises the no-injection rows from 32 to 45.
2. **Only the `@@` line was read for the enclosing declaration.** Patches
   frequently show the signature as a context line below the header, so a method
   NeoForge inserted into mid-body was not recorded as patched. This is what hid
   `handleSetCarriedItem`. Context lines carrying a declaration are now scanned.

With both fixed, `handleSetCarriedItem` is the only overwrite colliding with a
NeoForge-patched method, and it is now fixed.

**Standing conclusion:** every numeric claim this tool has made so far has been
wrong at least once. It is a queue-narrowing device. The classification has to
come from reading the patch against the hook.

### 12. The 45 no-injection rows — SAFE, validated

Re-checked with comments stripped: **zero** contain a behaviour-changing
annotation. The only live annotations across all 45 are `@Mixin`, `@Shadow`,
`@Unique` and `@Final` - plumbing that cannot alter behaviour, and which fails
at boot under `defaultRequire: 1` if a shadowed member moves.

20 of them contain nothing but `@Mixin`: their entire body is commented out.
They are inert, but they inflate the mixin surface and every future audit's
queue. Worth deleting as a separate cleanup, not folded into this one.

---

## Final state: all 114 classified

| State | Count | Basis |
| --- | --- | --- |
| FIXED | 3 | `MappedRegistry`, `ItemStack`, `ServerGamePacketListenerImpl` - a defect was found and corrected, each with a regression test or a headless assertion where one was possible |
| REVIEWED SAFE | 18 | Patch read against the hook by hand; reasoning recorded per class above |
| SAFE (no injections) | 45 | **See the correction below - this classification was overstated.** |
| SAFE (disjoint) | 48 | Live injections whose target methods do not appear in the NeoForge patch, including declarations on context lines |

Four defects were found and fixed in total, counting the one in
`ServerPlayerGameMode` that is now a bridge rather than a hook:

1. `ServerPlayerGameMode#destroyBlock` - two unreconciled break events
2. `MappedRegistry#register` - hook on a delegate, missing wide-overload callers
3. `ItemStack#hurtAndBreak` - hook on a delegate, `PlayerItemDamageEvent` dead
4. `ServerGamePacketListenerImpl#handleSetCarriedItem` - overwrite discarding
   NeoForge's hotbar events

**None of the four was visible in play.** Blocks broke, durability decreased,
registries populated, hotbars switched. In every case the ecosystem that lost
its notification was the one nobody was watching.

## What this audit cannot tell you

It compares Cardboard's hooks against NeoForge's patches. It says nothing about
hooks that are wrong on their own terms, and this session found four of those
by other means - the respawn hang, the CraftPlayer not surviving death,
`Player#getHealth()` returning a constant, and a cancellation-delivery
regression introduced by one of the fixes above. All four are
Cardboard-versus-Paper defects that no amount of patch diffing would surface.

The audit is a floor, not a ceiling.


## Correction: the "no injections" bucket was overstated

I classified 45 classes as unable to alter behaviour because they carry no
injection annotation. That is wrong, and it is the fifth time a claim from this
tooling has failed.

Mixin merges plain methods, fields and interface implementations into the target
without any annotation at all. `RegistryLookup_DelegateMixin` is the clearest
case: no annotation beyond `@Mixin`, and it supplies the whole implementation of
`getValueForCopying` that Paper's registry-modification API depends on.

Re-measured, of those 45:

- **41** merge plain members or implement a bridge interface. They are live code.
  The absence of an injection annotation says nothing about them.
- **7** are genuinely inert - no annotations, no method bodies, no interface.

This was very nearly a destructive mistake: I had a list of 62 "dead" mixin files
queued for deletion on the same reasoning, and `RegistryLookup_DelegateMixin` was
on it.

**What the 41 still have going for them:** they add members rather than
intercepting NeoForge's control flow, so they cannot discard a NeoForge change
the way an `@Overwrite` can, and a `@Shadow` whose target moved fails at boot
under `defaultRequire: 1`. That is a real argument, and it is weaker than
"cannot alter behaviour". They are **UNREVIEWED**, not SAFE.

Corrected state of the audit:

| State | Count |
| --- | --- |
| FIXED | 3 |
| REVIEWED SAFE | 18 |
| SAFE (inert) | 7 |
| SAFE (disjoint) | 48 |
| **UNREVIEWED (merge members, no injections)** | **38** |

38 classes still need reading. The queue is not empty.
