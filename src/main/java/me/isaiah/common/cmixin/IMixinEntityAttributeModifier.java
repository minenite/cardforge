package me.isaiah.common.cmixin;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public interface IMixinEntityAttributeModifier {

    public double IC$get_value();

    public AttributeModifier.Operation IC$get_operation();

    public UUID IC$get_uuid();

}
