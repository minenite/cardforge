package org.bukkit.craftbukkit.structure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

/**
 * Loading, saving and registration of saved structures, backed by the server's
 * own structure template manager so structure blocks and the API see the same
 * set of structures.
 */
public class CraftStructureManager implements StructureManager {

    private final StructureTemplateManager handle;
    private final File folder;

    public CraftStructureManager(StructureTemplateManager handle, File worldContainer) {
        this.handle = handle;
        this.folder = new File(worldContainer, "generated");
    }

    private Identifier toMinecraft(NamespacedKey key) {
        Preconditions.checkArgument(key != null, "NamespacedKey cannot be null");
        return CraftNamespacedKey.toMinecraft(key);
    }

    @Override
    public Map<NamespacedKey, Structure> getStructures() {
        Map<NamespacedKey, Structure> structures = new HashMap<>();
        this.handle.listTemplates().forEach(id ->
                this.handle.get(id).ifPresent(template ->
                        structures.put(CraftNamespacedKey.fromMinecraft(id), new CraftStructure(template))));
        return structures;
    }

    @Override
    public Structure getStructure(NamespacedKey key) {
        return this.handle.get(this.toMinecraft(key)).map(CraftStructure::new).orElse(null);
    }

    @Override
    public Structure registerStructure(NamespacedKey key, Structure structure) {
        Preconditions.checkArgument(structure != null, "Structure cannot be null");
        // getOrCreate registers an empty template under the key; its contents are
        // then replaced with the caller's, which is the only way in without
        // reaching past the manager's own map.
        StructureTemplate registered = this.handle.getOrCreate(this.toMinecraft(key));
        CompoundTag saved = ((CraftStructure) structure).getHandle().save(new CompoundTag());
        registered.load(net.minecraft.core.registries.BuiltInRegistries.BLOCK, saved);
        return new CraftStructure(registered);
    }

    @Override
    public Structure unregisterStructure(NamespacedKey key) {
        Structure previous = this.getStructure(key);
        this.handle.remove(this.toMinecraft(key));
        return previous;
    }

    @Override
    public Structure loadStructure(NamespacedKey key) {
        return this.loadStructure(key, true);
    }

    @Override
    public Structure loadStructure(NamespacedKey key, boolean register) {
        if (register) {
            // getOrCreate both loads from disk and registers, which is exactly
            // what the registering form of this call means.
            return new CraftStructure(this.handle.getOrCreate(this.toMinecraft(key)));
        }

        File file = this.getStructureFile(key);
        if (!file.exists()) return null;
        try {
            return this.loadStructure(file);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load structure " + key, ex);
        }
    }

    @Override
    public void saveStructure(NamespacedKey key) {
        this.handle.save(this.toMinecraft(key));
    }

    @Override
    public void saveStructure(NamespacedKey key, Structure structure) throws IOException {
        Preconditions.checkArgument(structure != null, "Structure cannot be null");
        File file = this.getStructureFile(key);
        file.getParentFile().mkdirs();
        this.saveStructure(file, structure);
    }

    @Override
    public void deleteStructure(NamespacedKey key) throws IOException {
        this.deleteStructure(key, true);
    }

    @Override
    public void deleteStructure(NamespacedKey key, boolean unregister) throws IOException {
        if (unregister) {
            this.handle.remove(this.toMinecraft(key));
        }
        File file = this.getStructureFile(key);
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not delete structure file " + file);
        }
    }

    @Override
    public File getStructureFile(NamespacedKey key) {
        Identifier id = this.toMinecraft(key);
        return new File(new File(this.folder, id.getNamespace()), "structures/" + id.getPath() + ".nbt");
    }

    @Override
    public Structure loadStructure(File file) throws IOException {
        Preconditions.checkArgument(file != null, "File cannot be null");
        try (InputStream stream = new java.io.FileInputStream(file)) {
            return this.loadStructure(stream);
        }
    }

    @Override
    public Structure loadStructure(InputStream stream) throws IOException {
        Preconditions.checkArgument(stream != null, "InputStream cannot be null");
        CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        StructureTemplate template = new StructureTemplate();
        template.load(net.minecraft.core.registries.BuiltInRegistries.BLOCK, tag);
        return new CraftStructure(template);
    }

    @Override
    public void saveStructure(File file, Structure structure) throws IOException {
        Preconditions.checkArgument(file != null, "File cannot be null");
        file.getParentFile().mkdirs();
        try (OutputStream stream = new java.io.FileOutputStream(file)) {
            this.saveStructure(stream, structure);
        }
    }

    @Override
    public void saveStructure(OutputStream stream, Structure structure) throws IOException {
        Preconditions.checkArgument(stream != null, "OutputStream cannot be null");
        Preconditions.checkArgument(structure != null, "Structure cannot be null");
        NbtIo.writeCompressed(((CraftStructure) structure).getHandle().save(new CompoundTag()), stream);
    }

    @Override
    public Structure createStructure() {
        return new CraftStructure(new StructureTemplate());
    }

    @Override
    public Structure copy(Structure structure) {
        Preconditions.checkArgument(structure != null, "Structure cannot be null");
        StructureTemplate copy = new StructureTemplate();
        copy.load(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                ((CraftStructure) structure).getHandle().save(new CompoundTag()));
        return new CraftStructure(copy);
    }
}
