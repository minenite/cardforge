package org.minenite.cardforge.mixin.invoker;

import java.util.List;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A structure template keeps its palettes and entity list private, and the API
 * needs both to report what a saved structure contains.
 */
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {

    @Accessor("palettes")
    List<StructureTemplate.Palette> cardforge$getPalettes();

    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> cardforge$getEntityInfoList();
}
