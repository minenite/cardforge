package org.bukkit.craftbukkit.block;

import org.bukkit.Location;
import org.bukkit.World;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A generic TileState for block entities registered by a NeoForge mod.
 *
 * CraftBukkit maps every vanilla block-entity type to a purpose-built state
 * class, and treats an unmapped type as a bug: the default factory asserts the
 * block entity is null and throws "Unexpected BlockState" otherwise. That is the
 * right call for vanilla, where a missing mapping really is an oversight, but it
 * is wrong here - a modded block entity has no CraftBukkit class and never will.
 *
 * Left alone, that assertion turns any modded block entity into a crash for
 * every plugin that calls Block#getState, which includes anything scanning a
 * region: world editors, protection plugins, chunk analysers. Handing back a
 * plain TileState instead means such plugins keep working, and still get the
 * location, type and persistent data container. What they do not get is typed
 * access to the mod's own contents, which no Bukkit interface could describe
 * anyway.
 */
public class CraftModdedBlockEntity extends CraftBlockEntityState<BlockEntity> {

    public CraftModdedBlockEntity(World world, BlockEntity blockEntity) {
        super(world, blockEntity);
    }

    protected CraftModdedBlockEntity(CraftModdedBlockEntity state, Location location) {
        super(state, location);
    }

    @Override
    public CraftModdedBlockEntity copy() {
        return new CraftModdedBlockEntity(this, null);
    }

    @Override
    public CraftModdedBlockEntity copy(Location location) {
        return new CraftModdedBlockEntity(this, location);
    }
}
