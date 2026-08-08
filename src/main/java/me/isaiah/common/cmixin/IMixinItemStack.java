package me.isaiah.common.cmixin;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 */
public interface IMixinItemStack {

	/**
	 */
	void IC$damage(int amount, LivingEntity entity, InteractionHand hand);
	
	/**
	 */
	void IC$modify_arguments(String arguments);
	
	/**
	 */
	public List<MobEffectInstance> IC$get_potion_status_effects();

}
