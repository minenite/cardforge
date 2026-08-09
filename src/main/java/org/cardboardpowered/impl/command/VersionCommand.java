package org.cardboardpowered.impl.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.StringUtil;
import org.minenite.cardforge.BuildInfo;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class VersionCommand extends Command {

    /** The repository this build is compared against when checking for updates. */
    public static String REPOSITORY = "minenite/cardforge";
    public static String BRANCH = "main";

    public VersionCommand(String name) {
        super(name);

        this.description = "Gets the version of this server including any plugins in use";
        this.usageMessage = "/version [plugin name]";
        this.setPermission("bukkit.command.version");
        this.setAliases(Arrays.asList("ver", "about", "version"));
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            // Was modVersion("cardboard"), a mod id that does not exist here - it
            // resolved to the literal "unknown" on every server. The build stamp is
            // the authoritative answer and needs no registry lookup at all.
            String ver = CraftServer.INSTANCE.getShortVersion();

            String message = "This server is running " + ChatColor.GOLD + Bukkit.getName() + ChatColor.RESET + " version " + ver + ChatColor.ITALIC + " (Implementing API version " + Bukkit.getBukkitVersion() + ")";
            sender.sendMessage(message);
            sendVersion(sender);
        } else {
            StringBuilder name = new StringBuilder();

            for (String arg : args) {
                if (name.length() > 0) name.append(' ');
                name.append(arg);
            }

            String pluginName = name.toString();
            Plugin exactPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (exactPlugin != null) {
                describeToSender(exactPlugin, sender);
                return true;
            }

            boolean found = false;
            pluginName = pluginName.toLowerCase(java.util.Locale.ENGLISH);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.getName().toLowerCase(java.util.Locale.ENGLISH).contains(pluginName)) {
                    describeToSender(plugin, sender);
                    found = true;
                }
            }

            if (!found) {
                sender.sendMessage("This server is not running any plugin by that name.");
                sender.sendMessage("Use /plugins to get a list of plugins.");
            }
        }
        return true;
    }

    private void describeToSender(Plugin plugin, CommandSender sender) {
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion());

        if (desc.getDescription() != null) sender.sendMessage(desc.getDescription());
        if (desc.getWebsite() != null)     sender.sendMessage("Website: " + ChatColor.GREEN + desc.getWebsite());
        if (!desc.getAuthors().isEmpty())  sender.sendMessage((desc.getAuthors().size() == 1 ? ("Author: ") : ("Authors: ")) + getAuthors(desc));
    }

    private String getAuthors(final PluginDescriptionFile desc) {
        StringBuilder result = new StringBuilder();
        List<String> authors = desc.getAuthors();

        for (int i = 0; i < authors.size(); i++) {
            if (result.length() > 0) {
                result.append(ChatColor.WHITE);
                result.append(i < authors.size() - 1 ? ", " : " and ");
            }

            result.append(ChatColor.GREEN);
            result.append(authors.get(i));
        }

        return result.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String toComplete = args[0].toLowerCase(java.util.Locale.ENGLISH);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
                if (StringUtil.startsWithIgnoreCase(plugin.getName(), toComplete))
                    completions.add(plugin.getName());

            return completions;
        }
        return ImmutableList.of();
    }

    private final ReentrantLock versionLock = new ReentrantLock();
    private boolean hasVersion = false;
    private String versionMessage = null;
    private final Set<CommandSender> versionWaiters = new HashSet<>();
    private boolean versionTaskStarted = false;
    private long lastCheck = 0;

    private void sendVersion(CommandSender sender) {
        if (hasVersion) {
            if (System.currentTimeMillis() - lastCheck > 21600000) {
                lastCheck = System.currentTimeMillis();
                hasVersion = false;
            } else {
                sender.sendMessage(versionMessage);
                return;
            }
        }
        versionLock.lock();
        try {
            if (hasVersion) {
                sender.sendMessage(versionMessage);
                return;
            }
            versionWaiters.add(sender);
            sender.sendMessage("Checking version, please wait...");
            if (!versionTaskStarted) {
                versionTaskStarted = true;
                new Thread(this::obtainVersion).start();
            }
        } finally {
            versionLock.unlock();
        }
    }

    /**
     * Describes this build honestly on the second line of /version.
     *
     * <p>Cardboard gated the update check on the version string starting with
     * "git-Cardboard-", and since nothing here ever produced that prefix, every
     * server fell through to "Unknown version, custom build?" regardless of what
     * it was actually running. The gate is now the one fact that decides whether a
     * check is even meaningful: whether the jar knows which commit built it.
     */
    private void obtainVersion() {
        if (BuildInfo.isUnknownBuild()) {
            setVersionMessage(ChatColor.RED + "Unknown build - this jar was not built from a git checkout.");
            return;
        }

        String built = BuildInfo.BRANCH + "@" + BuildInfo.shortCommit() + ", built " + BuildInfo.BUILD_TIME;

        if (BuildInfo.DIRTY) {
            // Uncommitted changes mean the commit does not describe the jar, so
            // comparing it against the remote would report on code that is not here.
            setVersionMessage(ChatColor.RED + "Development build with uncommitted changes (" + built + ")");
            return;
        }

        int behind = check();
        switch (behind) {
            case 0 -> setVersionMessage(ChatColor.GREEN + "You are running the latest version (" + built + ")");
            case -1 -> setVersionMessage(ChatColor.RED + "You are running an unreleased build ahead of " + BRANCH + " (" + built + ")");
            case -4 -> setVersionMessage(ChatColor.RED + "This build's commit has diverged from " + BRANCH + " (" + built + ")");
            case -2 -> setVersionMessage(ChatColor.RED + "This commit is not on " + REPOSITORY + " (" + built + ")");
            case -3 -> setVersionMessage(ChatColor.RED + "Could not reach GitHub to check for updates (" + built + ")");
            default -> setVersionMessage(ChatColor.RED + "You are " + behind + " version(s) behind (" + built + ")");
        }
    }

    private void setVersionMessage(String msg) {
        lastCheck = System.currentTimeMillis();
        versionMessage = msg;
        versionLock.lock();
        try {
            hasVersion = true;
            versionTaskStarted = false;
            for (CommandSender sender : versionWaiters)
                sender.sendMessage(versionMessage);
            versionWaiters.clear();
        } finally {
            versionLock.unlock();
        }
    }
    
    public static String getGitHash() {
        return BuildInfo.COMMIT;
    }

    public static boolean isDirty() {
        return BuildInfo.DIRTY;
    }

    public static int check() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPOSITORY + "/compare/" + BRANCH + "..." + getGitHash()).openConnection();
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) return -2; // Unknown commit

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
            String status = obj.get("status").getAsString();

            if (status.equalsIgnoreCase("identical")) return 0;
            if (status.equalsIgnoreCase("behind")) return obj.get("behind_by").getAsInt();
            if (status.equalsIgnoreCase("ahead")) return -1;

            // "diverged" is the ordinary result after history is rewritten, and it
            // is not the same thing as being ahead - the commit is genuinely not an
            // ancestor or descendant of the branch any more.
            return -4;
        } catch (IOException | RuntimeException e) {
            // An offline server is a normal condition, not something to dump a
            // stack trace over; the caller turns -3 into a readable line.
            return -3;
        }
    }

}