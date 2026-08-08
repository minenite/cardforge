package io.papermc.paper.world.saveddata;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.cardboardpowered.IdentifierExtra;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PaperWorldPDC extends SavedData {
    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public static final Codec<PaperWorldPDC> CODEC = CraftPersistentDataContainer.createCodec(DATA_TYPE_REGISTRY)
        .xmap(PaperWorldPDC::new, PaperWorldPDC::persistentData);
    // No DataFixTypes on purpose. This container holds arbitrary plugin data, so
    // Minecraft's datafixers must never be run over it. Cardboard expressed that on
    // Fabric by using extend-enum to add a null-typed DataFixTypes.PAPER_NONE, but
    // that is a Fabric-only mechanism and is not needed here: 26.2's SavedDataType
    // has a constructor that leaves the fix type null, and SavedDataStorage skips
    // the datafixer outright when it is. Passing a real type such as LEVEL, as an
    // earlier port did, would silently run level datafixes across plugin data.
    public static final SavedDataType<PaperWorldPDC> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(IdentifierExtra.PAPER_NAMESPACE, "persistent_data_container"),
        () -> new PaperWorldPDC(new CraftPersistentDataContainer(DATA_TYPE_REGISTRY)),
        CODEC
    );

    private final CraftPersistentDataContainer persistentData;

    public PaperWorldPDC(final CraftPersistentDataContainer persistentData) {
        this.persistentData = persistentData;
    }

    public CraftPersistentDataContainer persistentData() {
        return this.persistentData;
    }

    public void setFrom(final CraftPersistentDataContainer source) {
        if (!Objects.equals(this.persistentData, source)) {
            this.persistentData.clear();
            this.persistentData.putAll(source.getTagsCloned());
            this.setDirty();
        }
    }
}
