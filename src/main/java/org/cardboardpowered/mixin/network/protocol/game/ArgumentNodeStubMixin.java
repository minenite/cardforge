package org.cardboardpowered.mixin.network.protocol.game;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces non-vanilla command argument types with a plain string when the
 * server is behind a proxy.
 *
 * <p>A proxy has to decode the command tree, because it merges its own commands
 * with the backend's. Brigadier argument types cross the wire as numeric ids
 * into the server's registry, and a proxy keeps a hardcoded table of the vanilla
 * ones. NeoForge registers two of its own - {@code neoforge:enum} and
 * {@code neoforge:modid} - so the first command tree a NeoForge server sends
 * kills the connection:
 *
 * <pre>
 * IllegalArgumentException: Argument type identifier 58 unknown.
 *   at ArgumentPropertyRegistry.readIdentifier
 *   at AvailableCommandsPacket.decode
 * </pre>
 *
 * <p>Verified against Velocity 3.5.1 and 4.0.0, which fail identically. It is
 * not mod-specific: any NeoForge server hits it, because the types come from
 * NeoForge itself.
 *
 * <p>Writing those arguments as {@code brigadier:string} keeps the tree
 * decodable by anything that understands vanilla. The cost is that client-side
 * tab completion for those arguments falls back to free text - the commands
 * themselves still work, and the server still parses them with the real type,
 * because only the wire representation changes.
 *
 * <p>Only active when {@code settings.bungeecord} is enabled in spigot.yml, so a
 * server that is not proxied sends its command tree exactly as before.
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public class ArgumentNodeStubMixin {

    /** Namespaces a vanilla-only proxy can be expected to understand. */
    @Unique
    private static boolean cardboard$isProxySafe(ArgumentTypeInfo<?, ?> info) {
        Identifier key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info);
        if (key == null) {
            return false;
        }
        String namespace = key.getNamespace();
        return "minecraft".equals(namespace) || "brigadier".equals(namespace);
    }

    @Redirect(
            method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket$ArgumentNodeStub;"
                            + "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;"
                            + "Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V"))
    private static void cardboard$substituteUnknownArgumentTypes(FriendlyByteBuf buffer,
                                                                 ArgumentTypeInfo.Template<?> template) {
        ArgumentTypeInfo.Template<?> toWrite = template;
        if (org.spigotmc.SpigotConfig.bungee && !cardboard$isProxySafe(template.type())) {
            // greedyString rather than word: a substituted argument may legitimately
            // contain spaces, and the server parses with the real type regardless.
            toWrite = ArgumentTypeInfos.unpack(StringArgumentType.greedyString());
        }
        cardboard$serializeCap(buffer, toWrite);
    }

    /**
     * Calls the target's own private serializer. Reimplementing it here would mean
     * duplicating the id-and-properties wire format, which is exactly the kind of
     * copy that goes stale the next time the format moves.
     */
    @Unique
    @SuppressWarnings("unchecked")
    private static <A extends com.mojang.brigadier.arguments.ArgumentType<?>> void cardboard$serializeCap(
            FriendlyByteBuf buffer, ArgumentTypeInfo.Template<A> template) {
        ArgumentTypeInfo<A, ArgumentTypeInfo.Template<A>> info =
                (ArgumentTypeInfo<A, ArgumentTypeInfo.Template<A>>) template.type();
        buffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(info));
        info.serializeToNetwork(template, buffer);
    }
}
