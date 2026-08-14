package io.papermc.paper.datapack;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.bukkit.FeatureFlag;
import org.bukkit.craftbukkit.CraftFeatureFlag;

/**
 * One datapack the server has discovered, enabled or not.
 */
public class PaperDatapack implements Datapack {

    private final Pack handle;
    private final PackRepository repository;

    public PaperDatapack(Pack handle, PackRepository repository) {
        this.handle = handle;
        this.repository = repository;
    }

    @Override
    public String getName() {
        return this.handle.getId();
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.getTitle());
    }

    @Override
    public net.kyori.adventure.text.Component getDescription() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.getDescription());
    }

    @Override
    public net.kyori.adventure.text.Component computeDisplayName() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.getChatLink(this.isEnabled()));
    }

    @Override
    public boolean isRequired() {
        return this.handle.isRequired();
    }

    @Override
    public Compatibility getCompatibility() {
        return switch (this.handle.getCompatibility()) {
            case TOO_OLD -> Compatibility.TOO_OLD;
            case TOO_NEW -> Compatibility.TOO_NEW;
            default -> Compatibility.COMPATIBLE;
        };
    }

    @Override
    public Set<FeatureFlag> getRequiredFeatures() {
        return CraftFeatureFlag.getFromNMS(this.handle.getRequestedFeatures()).stream()
                .map(FeatureFlag.class::cast).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public DatapackSource getSource() {
        // The pack source is a vanilla singleton with no id to match on, so this
        // reports where the pack physically came from instead: the ids the server
        // itself uses for its built-in packs.
        String id = this.handle.getId();
        if ("vanilla".equals(id)) return DatapackSource.DEFAULT;
        if (id.startsWith("file/")) return DatapackSource.WORLD;
        if (id.startsWith("mod:") || id.startsWith("neoforge")) return DatapackSource.PLUGIN;
        return DatapackSource.BUILT_IN;
    }

    @Override
    public boolean isEnabled() {
        return this.repository.getSelectedIds().contains(this.handle.getId());
    }

    @Override
    public void setEnabled(boolean enabled) {
        java.util.Set<String> selected = new java.util.LinkedHashSet<>(this.repository.getSelectedIds());
        if (enabled ? !selected.add(this.handle.getId()) : !selected.remove(this.handle.getId())) {
            return;
        }
        // Selecting a pack only stages it; the resource reload is what actually
        // applies its recipes, loot tables and tags.
        this.repository.setSelected(selected);
        org.bukkit.Bukkit.getServer().reloadData();
    }
}
