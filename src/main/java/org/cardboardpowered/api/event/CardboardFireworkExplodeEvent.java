package org.cardboardpowered.api.event;

import org.minenite.cardforge.event.Event;
import org.minenite.cardforge.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;

public interface CardboardFireworkExplodeEvent {

    Event<CardboardFireworkExplodeEvent> EVENT = EventFactory.createArrayBacked(CardboardFireworkExplodeEvent.class,
            (listeners) -> (firework) -> {
                for (CardboardFireworkExplodeEvent listener : listeners) {
                    InteractionResult result = listener.interact(firework);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(FireworkRocketEntity firework);
}
