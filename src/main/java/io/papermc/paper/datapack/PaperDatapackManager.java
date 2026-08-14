package io.papermc.paper.datapack;

import java.util.Collection;
import java.util.stream.Collectors;

import net.minecraft.server.packs.repository.PackRepository;

/**
 * The server's view of its datapacks.
 */
public class PaperDatapackManager implements DatapackManager {

    private final PackRepository repository;

    public PaperDatapackManager(PackRepository repository) {
        this.repository = repository;
    }

    @Override
    public void refreshPacks() {
        this.repository.reload();
    }

    @Override
    public Datapack getPack(String name) {
        net.minecraft.server.packs.repository.Pack pack = this.repository.getPack(name);
        return pack == null ? null : new PaperDatapack(pack, this.repository);
    }

    @Override
    public Collection<Datapack> getPacks() {
        return this.repository.getAvailablePacks().stream()
                .map(pack -> (Datapack) new PaperDatapack(pack, this.repository))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<Datapack> getEnabledPacks() {
        return this.repository.getSelectedPacks().stream()
                .map(pack -> (Datapack) new PaperDatapack(pack, this.repository))
                .collect(Collectors.toUnmodifiableList());
    }
}
