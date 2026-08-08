package org.minenite.cardforge.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.minenite.cardforge.api.CardForge;
import org.minenite.cardforge.api.ModInfo;
import org.minenite.cardforge.api.ModdedRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * The bridge implementation.
 *
 * <p>Deliberately thin: every method here either reads NeoForge's own state or
 * hands a NeoForge object straight back. Nothing is cached that NeoForge already
 * owns, and nothing is reimplemented.
 */
public final class CardForgeImpl implements CardForge {

    private static final CardForgeImpl INSTANCE = new CardForgeImpl();

    private final ModdedRegistry registry = new ModdedRegistryImpl();

    private CardForgeImpl() {
    }

    public static CardForge get() {
        return INSTANCE;
    }

    public static Optional<CardForge> getIfPresent() {
        return Optional.of(INSTANCE);
    }

    @Override
    public String neoForgeVersion() {
        return ModList.get().getModContainerById("neoforge")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public String minecraftVersion() {
        return ModList.get().getModContainerById("minecraft")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public Collection<ModInfo> mods() {
        List<ModInfo> out = new ArrayList<>();
        ModList.get().getMods().forEach(info ->
                out.add(new ModInfo(info.getModId(), info.getDisplayName(), info.getVersion().toString())));
        return List.copyOf(out);
    }

    @Override
    public Optional<ModInfo> mod(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo())
                .map(i -> new ModInfo(i.getModId(), i.getDisplayName(), i.getVersion().toString()));
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public ModdedRegistry registry() {
        return this.registry;
    }

    @Override
    public <T> Optional<T> blockCapability(Block block,
                                           BlockCapability<T, net.minecraft.core.Direction> capability,
                                           BlockFace side) {
        return blockCapability(block, capability, side == null ? null : CraftBlock.blockFaceToNotch(side));
    }

    @Override
    public <T, C> Optional<T> blockCapability(Block block, BlockCapability<T, C> capability, C context) {
        if (block == null || capability == null) {
            return Optional.empty();
        }
        ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) block.getWorld()).getHandle();
        BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        // NeoForge does the resolution, including whether the block entity is
        // loaded and whether the mod actually provides the capability here.
        return Optional.ofNullable(level.getCapability(capability, pos, context));
    }
}
