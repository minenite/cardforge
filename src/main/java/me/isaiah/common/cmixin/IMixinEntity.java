package me.isaiah.common.cmixin;

import java.util.UUID;

import me.isaiah.common.entity.IEntity;
import me.isaiah.common.entity.IRemoveReason;
import me.isaiah.common.fabric.entity.FabricArmorStandEntity;
import me.isaiah.common.fabric.entity.FabricEntity;
import me.isaiah.common.fabric.entity.FabricPlayer;
import me.isaiah.common.fabric.entity.FabricPrimedTnt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IMixinEntity {

    public default Entity IgetMCEntity() {
        return (Entity)(Object)this;
    }

    public IEntity getAsICommon();

    public default IEntity newICommonInstance_InternalOnly() {
        Entity mc = IgetMCEntity();
        if (mc instanceof Player) {
            if (mc instanceof ServerPlayer) {  }
            else {  } // TODO
            return new FabricPlayer(mc);
        }
        else if (mc instanceof PrimedTnt) { return new FabricPrimedTnt(mc); }
        else if (mc instanceof ArmorStand) {
            return new FabricArmorStandEntity(mc);
        }
        return new FabricEntity(mc);
    }

    /**
     * @reason 1.16 & 1.17 differ in entity removal
     * @deprecated We only support 1.18+
     */
    @Deprecated
    public void Iremove(IRemoveReason r);

    /**
     * @reason 1.16 requires an UUID to be sent
     */
    public void IsendText(Component text, UUID id);

    /**
     * 1.16 - removed
     * 1.17 - isRemoved()
     * @deprecated We only support 1.18+
     */
    @Deprecated
    public boolean ic_isRemoved();

    /**
     */
    public boolean IC$has_status_effect(MobEffect effect);
    
    /**
     */
    public void IC$add_status_effect(MobEffect effect, int duration, int amp, boolean ambient, boolean particles);
    

    /**
     */
    public void IC$remove_status_effect(MobEffect effect);
    
    /**
     */
    public MobEffectInstance IC$get_status_effect(int type);
    
    /**
     */
    public int IC$get_status_effect_id(MobEffectInstance effect);
    
    /**
     * <= 1.20.6: Entity.teleport(x,y,z)
     * >= 1.20: Entity.teleportTo(TeleportTarget)
     */
    public void IC$teleport(ServerLevel world, double x, double y, double z);
    
    /**
     * You Just Had to Break getWorld()...
     * https://fabricmc.net/2025/09/23/1219.html
     */
    public Level ic$getWorld();

}