package org.cardboardpowered.api.event;

import org.minenite.cardforge.event.Event;
import org.minenite.cardforge.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

public interface CardboardEntityMountEvent {

    Event<CardboardEntityMountEvent> EVENT = EventFactory.createArrayBacked(CardboardEntityMountEvent.class,
            (listeners) -> (vehicle, entity) -> {
                for (CardboardEntityMountEvent listener : listeners) {
                    InteractionResult result = listener.interact(vehicle, entity);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(Entity vehicle, Entity entity);

}
