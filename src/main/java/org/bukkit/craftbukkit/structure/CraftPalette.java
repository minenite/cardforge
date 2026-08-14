package org.bukkit.craftbukkit.structure;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.block.BlockState;
import org.bukkit.structure.Palette;

/**
 * One block variant set of a structure, as saved by a structure block.
 */
public class CraftPalette implements Palette {

    private final List<StructureTemplate.StructureBlockInfo> blocks;

    public CraftPalette(List<StructureTemplate.StructureBlockInfo> blocks) {
        this.blocks = blocks;
    }

    @Override
    public List<BlockState> getBlocks() {
        // Unplaced states: the positions are structure-relative, so there is no
        // world to attach them to until the structure is placed.
        return this.blocks.stream()
                .map(info -> org.bukkit.craftbukkit.block.CraftBlockStates.getBlockState(
                        org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry(),
                        info.pos(), info.state(), info.nbt()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public int getBlockCount() {
        return this.blocks.size();
    }
}
