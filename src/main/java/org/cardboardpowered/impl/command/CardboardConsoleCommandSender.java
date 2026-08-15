package org.cardboardpowered.impl.command;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.cardboardpowered.CardboardConfig;
import org.cardboardpowered.CardboardLogger;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class CardboardConsoleCommandSender implements ConsoleCommandSender, CommandSender {

    /**
     * Attachments handed out to plugins. The console holds every permission
     * regardless, but the attachments still have to be real objects: every
     * addAttachment here returned null, so a plugin that attached a permission
     * and later removed it died on the null it was given back.
     */
    private org.bukkit.permissions.PermissibleBase perm;

    /**
     * Built on first use, not in the constructor: this sender is created from
     * CraftServer's own constructor, and PermissibleBase immediately calls
     * Bukkit.getServer(), which is still null that early. Constructing it eagerly
     * crashed the server before it finished starting.
     */
    private org.bukkit.permissions.PermissibleBase perm() {
        if (this.perm == null) {
            this.perm = new org.bukkit.permissions.PermissibleBase(this);
        }
        return this.perm;
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public void sendMessage(String msg) {
        // BukkitLogger.getLogger().info(msg);
    	if (CardboardConfig.shouldStripConsoleColor) {
    		CardboardLogger.getSLF4J().info( ChatColor.stripColor(msg) );
    	}
    	CardboardLogger.getSLF4J().info(msg);
    }

    @Override
    public void sendMessage(String[] arg0) {
        for (String str : arg0) sendMessage(str);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return this.perm().addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return this.perm().addAttachment(plugin, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return this.perm().addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return this.perm().addAttachment(plugin, name, value, ticks);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return this.perm().getEffectivePermissions();
    }

    @Override
    public boolean hasPermission(String arg0) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission arg0) {
        return true;
    }

    @Override
    public boolean isPermissionSet(String arg0) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission arg0) {
        return true;
    }

    @Override
    public void recalculatePermissions() {
        this.perm().recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        this.perm().removeAttachment(attachment);
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean arg0) {
    }

    @Override
    public void abandonConversation(Conversation arg0) {
    }

    @Override
    public void abandonConversation(Conversation arg0, ConversationAbandonedEvent arg1) {
    }

    @Override
    public void acceptConversationInput(String arg0) {
    }

    @Override
    public boolean beginConversation(Conversation arg0) {
        return false;
    }

    @Override
    public boolean isConversing() {
        return false;
    }

    @Override
    public void sendRawMessage(String msg) {
        Bukkit.getLogger().info(msg);
    }

    public void sendMessage(UUID uuid, String[] msg) {
        sendMessage(msg);
    }

    public void sendMessage(UUID uuid, String msg) {
        sendMessage(msg);
    }

    public void sendRawMessage(UUID uuid, String msg) {
        sendRawMessage(msg);
    }

    private final CommandSender.Spigot spigot = new CommandSender.Spigot() {

        @Override
        public void sendMessage(BaseComponent component) {
            CardboardConsoleCommandSender.this.sendMessage(TextComponent.toLegacyText(component));
        }

        @Override
        public void sendMessage(BaseComponent... components) {
            CardboardConsoleCommandSender.this.sendMessage(TextComponent.toLegacyText(components));
        }

        @Override
        public void sendMessage(UUID sender, BaseComponent... components) {
            this.sendMessage(components);
        }

        @Override
        public void sendMessage(UUID sender, BaseComponent component) {
            this.sendMessage(component);
        }
    };

    @Override
    public org.bukkit.command.CommandSender.Spigot spigot() {
        return spigot;
    }

    @Override
    public @NotNull Component name() {
        return Component.text("Console");
    }

}
