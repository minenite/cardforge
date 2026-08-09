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
