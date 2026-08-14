package org.cardboardpowered.bridge.world.entity.player;

public interface PlayerBridge {

    /**
     * Writes the direction damage came from. The field is protected in Player and
     * getHurtDir is a hardcoded zero everywhere else.
     */
    default void cardboard$setHurtDir(float hurtDir) {
    }
}
