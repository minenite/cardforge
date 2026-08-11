package org.cardboardpowered.mixin.server.network;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.cardboardpowered.bridge.server.network.ClientBrandBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.2: capture the client brand from the vanilla minecraft:brand payload,
// which is what Paper exposes as PlayerCommonConnection#getClientBrandName.
@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin_Brand implements ClientBrandBridge {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    protected Connection connection;

    @Unique
    private String cardboard$clientBrand;

    /**
     * Brands seen on a connection, rather than on one listener.
     *
     * <p>The brand is sent during configuration and handled by the configuration
     * listener, which is a different object from the play listener a Player holds.
     * Storing it on the listener alone means the one anybody asks has never seen
     * it, and every client looks unknown.
     *
     * <p>The connection outlives both, so it is the thing to key on. Weak, so an
     * entry disappears with the connection rather than being tidied up by hand.
     */
    @Unique
    private static final Map<Connection, String> cardboard$brandsByConnection =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void cardboard$captureBrand(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof BrandPayload brandPayload) {
            this.cardboard$clientBrand = brandPayload.brand();
            if (this.connection != null) {
                cardboard$brandsByConnection.put(this.connection, brandPayload.brand());
            }
        }
    }

    @Override
    public String cardboard_getClientBrand() {
        if (this.cardboard$clientBrand != null) {
            return this.cardboard$clientBrand;
        }
        // Sent during configuration, so the play listener asking here will not have
        // seen it itself.
        return this.connection == null ? null : cardboard$brandsByConnection.get(this.connection);
    }

    @Override
    public void cardboard_setClientBrand(String brand) {
        this.cardboard$clientBrand = brand;
        if (this.connection != null) {
            cardboard$brandsByConnection.put(this.connection, brand);
        }
    }
}
