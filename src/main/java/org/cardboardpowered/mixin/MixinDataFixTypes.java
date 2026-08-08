package org.cardboardpowered.mixin;

import org.cardboardpowered.bridge.IDataFixTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;

import net.minecraft.util.datafix.DataFixTypes;

@Mixin(DataFixTypes.class)
public enum MixinDataFixTypes implements IDataFixTypes {

	PAPER_NONE(null);
	
	@Shadow
	private MixinDataFixTypes(final TypeReference type) {
		this.type = type;
	}
	
	@Override
	public TypeReference cardboard$getType() {
		if (null == this.type) {
			return null;
		}
		return this.type;
	}
	
	@Override
	public boolean cardboard$isTypeNull() {
		return null == this.type;
	}
	
	@Shadow
	private TypeReference type;
	
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private <T> void injectNoOpFix(
            DataFixer fixerUpper,
            Dynamic<T> input,
            int fromVersion,
            int toVersion,
            CallbackInfoReturnable<Dynamic<T>> cir
    ) {
        DataFixTypes self = (DataFixTypes) (Object) this;

        // Paper patch: if type == null, return input unchanged
        if (((IDataFixTypes)(Object)self).cardboard$isTypeNull()) {
            cir.setReturnValue(input);
        }
    }
	
}
