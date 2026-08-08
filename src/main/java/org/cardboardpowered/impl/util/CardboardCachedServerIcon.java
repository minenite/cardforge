package org.cardboardpowered.impl.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.apache.commons.lang.Validate;
import org.bukkit.util.CachedServerIcon;

/**
 * Cardboard Bukkit Re-implementation of CachedServerIcon.
 * 
 * @implSpec Reimplementing CraftIconCache from Paper API 26.1.2
 */
public class CardboardCachedServerIcon implements CachedServerIcon {

    public byte[] value;

    public CardboardCachedServerIcon(byte[] value) {
        this.value = value;
    }

    @Override
    public String getData() {
        if (this.value != null) {
        	String head = "data:image/png;base64,";
        	return head + new String(java.util.Base64.getEncoder().encode(this.value), StandardCharsets.UTF_8);
		}
        return null;
    }
    
    /**
     * Cardboard: create a new CachedServerIcon from a BufferedImage. The image must be 64x64 pixels.
     */
    public static CardboardCachedServerIcon createFromImage(BufferedImage image) throws Exception {
        Validate.isTrue(image.getWidth() == 64 && image.getHeight() == 64, "Error: not 64*64");

        ByteArrayOutputStream bytebuf = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", bytebuf);

        return new CardboardCachedServerIcon(bytebuf.toByteArray());
    }
    
    /**
     * Cardboard: Create a new CachedServerIcon from a File. The image must be 64x64 pixels and in PNG format.
     */
    public static CardboardCachedServerIcon createFromFile(File file) throws Exception {
        return CardboardCachedServerIcon.createFromImage(ImageIO.read(file));
    }

}