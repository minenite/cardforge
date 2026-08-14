package org.bukkit.craftbukkit.structure;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.Location;
import org.bukkit.RegionAccessor;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.structure.Palette;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockTransformer;
import org.bukkit.util.BlockVector;
import org.bukkit.util.EntityTransformer;
import org.cardboardpowered.impl.world.CraftWorld;
import org.minenite.cardforge.mixin.invoker.StructureTemplateAccessor;

/**
 * A saved structure - the thing a structure block writes out - which can be
 * inspected and stamped back into a world.
 */
public class CraftStructure implements Structure {

    private final StructureTemplate structure;
    private final CraftPersistentDataContainer persistentDataContainer =
            new CraftPersistentDataContainer(new CraftPersistentDataTypeRegistry());

    public CraftStructure(StructureTemplate structure) {
        this.structure = structure;
    }

    public StructureTemplate getHandle() {
        return this.structure;
    }

    @Override
    public BlockVector getSize() {
        net.minecraft.core.Vec3i size = this.structure.getSize();
        return new BlockVector(size.getX(), size.getY(), size.getZ());
    }

    @Override
    public List<Palette> getPalettes() {
        return ((StructureTemplateAccessor) this.structure).cardforge$getPalettes().stream()
                .map(palette -> (Palette) new CraftPalette(palette.blocks()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public int getPaletteCount() {
        return ((StructureTemplateAccessor) this.structure).cardforge$getPalettes().size();
    }

    @Override
    public List<Entity> getEntities() {
        // Structure entities are stored as tags, not live entities, so there is
        // nothing to hand back until the structure is placed somewhere.
        return List.of();
    }

    @Override
    public int getEntityCount() {
        return ((StructureTemplateAccessor) this.structure).cardforge$getEntityInfoList().size();
    }

    @Override
    public void place(Location location, boolean includeEntities, StructureRotation rotation, Mirror mirror,
            int palette, float integrity, Random random) {
        Preconditions.checkArgument(location != null, "Location cannot be null");
        this.place(location.getWorld(), new BlockVector(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                includeEntities, rotation, mirror, palette, integrity, random);
    }

    @Override
    public void place(Location location, boolean includeEntities, StructureRotation rotation, Mirror mirror,
            int palette, float integrity, Random random, java.util.Collection<BlockTransformer> blockTransformers,
            java.util.Collection<EntityTransformer> entityTransformers) {
        // Transformers rewrite blocks and entities as they are placed. There is no
        // vanilla hook they map onto here, so rather than silently ignoring them
        // and producing a structure the caller did not ask for, this refuses.
        Preconditions.checkArgument(blockTransformers == null || blockTransformers.isEmpty(),
                "Block transformers are not supported");
        Preconditions.checkArgument(entityTransformers == null || entityTransformers.isEmpty(),
                "Entity transformers are not supported");
        this.place(location, includeEntities, rotation, mirror, palette, integrity, random);
    }

    @Override
    public void place(RegionAccessor regionAccessor, BlockVector location, boolean includeEntities,
            StructureRotation rotation, Mirror mirror, int palette, float integrity, Random random) {
        Preconditions.checkArgument(regionAccessor != null, "RegionAccessor cannot be null");
        Preconditions.checkArgument(location != null, "Location cannot be null");
        Preconditions.checkArgument(integrity >= 0.0F && integrity <= 1.0F, "Integrity must be between 0 and 1 inclusive");
        Preconditions.checkArgument(random != null, "Random cannot be null");

        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(!includeEntities);
        settings.setMirror(net.minecraft.world.level.block.Mirror.valueOf(mirror.name()));
        settings.setRotation(net.minecraft.world.level.block.Rotation.valueOf(rotation.name()));
        if (palette >= 0) {
            settings.setKnownShape(true);
        }

        net.minecraft.util.RandomSource source = new net.minecraft.world.level.levelgen.LegacyRandomSource(random.nextLong());
        settings.setRandom(source);
        if (integrity < 1.0F) {
            settings.clearProcessors().addProcessor(
                    new net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor(integrity));
        }

        this.structure.placeInWorld(
                ((CraftWorld) regionAccessor).getHandle(),
                new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                settings, source, net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    @Override
    public void place(RegionAccessor regionAccessor, BlockVector location, boolean includeEntities,
            StructureRotation rotation, Mirror mirror, int palette, float integrity, Random random,
            java.util.Collection<BlockTransformer> blockTransformers,
            java.util.Collection<EntityTransformer> entityTransformers) {
        Preconditions.checkArgument(blockTransformers == null || blockTransformers.isEmpty(),
                "Block transformers are not supported");
        Preconditions.checkArgument(entityTransformers == null || entityTransformers.isEmpty(),
                "Entity transformers are not supported");
        this.place(regionAccessor, location, includeEntities, rotation, mirror, palette, integrity, random);
    }

    @Override
    public void fill(Location corner1, Location corner2, boolean includeEntities) {
        Preconditions.checkArgument(corner1 != null, "corner1 cannot be null");
        Preconditions.checkArgument(corner2 != null, "corner2 cannot be null");
        Preconditions.checkArgument(corner1.getWorld() != null, "corner1 location has no world");
        Preconditions.checkArgument(corner1.getWorld().equals(corner2.getWorld()),
                "Corner locations must be in the same world");

        BlockVector origin = new BlockVector(
                Math.min(corner1.getBlockX(), corner2.getBlockX()),
                Math.min(corner1.getBlockY(), corner2.getBlockY()),
                Math.min(corner1.getBlockZ(), corner2.getBlockZ()));
        BlockVector size = new BlockVector(
                Math.abs(corner1.getBlockX() - corner2.getBlockX()) + 1,
                Math.abs(corner1.getBlockY() - corner2.getBlockY()) + 1,
                Math.abs(corner1.getBlockZ() - corner2.getBlockZ()) + 1);

        this.fill(corner1.getWorld(), origin, size, includeEntities);
    }

    @Override
    public void fill(Location origin, BlockVector size, boolean includeEntities) {
        Preconditions.checkArgument(origin != null, "Location cannot be null");
        Preconditions.checkArgument(origin.getWorld() != null, "Location has no world");
        this.fill(origin.getWorld(),
                new BlockVector(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()), size, includeEntities);
    }

    private void fill(org.bukkit.World world, BlockVector origin, BlockVector size, boolean includeEntities) {
        Preconditions.checkArgument(size != null, "Size cannot be null");
        Preconditions.checkArgument(size.getBlockX() > 0 && size.getBlockY() > 0 && size.getBlockZ() > 0,
                "Size must be at least 1x1x1");

        this.structure.fillFromWorld(
                ((CraftWorld) world).getHandle(),
                new BlockPos(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()),
                new net.minecraft.core.Vec3i(size.getBlockX(), size.getBlockY(), size.getBlockZ()),
                includeEntities, List.of());
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return this.persistentDataContainer;
    }
}
