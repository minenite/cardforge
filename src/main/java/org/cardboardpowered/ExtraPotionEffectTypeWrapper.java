package org.cardboardpowered;

import java.util.Map;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.jetbrains.annotations.NotNull;

public class ExtraPotionEffectTypeWrapper extends PotionEffectType {

	private int id;
	private NamespacedKey key;
	
	public ExtraPotionEffectTypeWrapper(int id, @NotNull String name) {
        // super(id, NamespacedKey.minecraft(name));
        
        this.id = id;
        this.key =  NamespacedKey.minecraft(name);
    }

    @Override
    public double getDurationModifier() {
        return this.requireType().getDurationModifier();
    }

    @NotNull
    @Override
    public String getName() {
        return this.requireType().getName();
    }

    /**
     * Get the potion type bound to this wrapper.
     *
     * @return The potion effect type
     */
    @NotNull
    public PotionEffectType getType() {
        // Resolve the real registered effect from the key. This used to return
        // "this", which made getName, isInstant, getColor and getDurationModifier
        // recurse into themselves forever the moment anyone called one.
        PotionEffectType resolved = org.bukkit.Registry.POTION_EFFECT_TYPE.get(this.key);
        return (resolved == null || resolved == this) ? null : resolved;
    }

    /** The registered effect, or a failure naming the key that could not be found. */
    @NotNull
    private PotionEffectType requireType() {
        PotionEffectType resolved = this.getType();
        if (resolved == null) {
            throw new IllegalStateException("No potion effect registered under " + this.key);
        }
        return resolved;
    }

    @Override
    public boolean isInstant() {
        return this.requireType().isInstant();
    }

    @NotNull
    @Override
    public Color getColor() {
        return this.requireType().getColor();
    }

	@Override
	public @NotNull String translationKey() {
		return this.requireType().translationKey();
	}

	@Override
	public double getAttributeModifierAmount(@NotNull Attribute attribute, int effectAmplifier) {
		return this.requireType().getAttributeModifierAmount(attribute, effectAmplifier);
	}

	@Override
	public @NotNull Map<Attribute, AttributeModifier> getEffectAttributes() {
		return this.requireType().getEffectAttributes();
	}

	@Override
	public @NotNull Category getEffectCategory() {
		return this.requireType().getEffectCategory();
	}

	@Override
	public @NotNull NamespacedKey getKey() {
		return this.key;
	}

	@Override
	public @NotNull PotionEffect createEffect(int duration, int amplifier) {
        return new PotionEffect(this, this.isInstant() ? 1 : (int) (duration * this.getDurationModifier()), amplifier);
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public @NotNull String getTranslationKey() {
		return this.translationKey();
	}

	@Override
	public @NotNull PotionEffectTypeCategory getCategory() {
		return this.requireType().getCategory();
	}
}
