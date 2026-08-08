package io.papermc.paper.plugin.entrypoint.classloader;

import io.papermc.paper.plugin.configuration.PluginMeta;

import org.cardboardpowered.plugin.MaterialValuesRewriter;

/**
 * Paper's plugin class-load transform hook.
 *
 * <p>Upstream uses this for its own bytecode fixups; CardForge uses it to
 * redirect Material.values() calls to a dynamic bridge, which is what lets a
 * precompiled plugin see materials added for NeoForge mod content. See
 * {@link MaterialValuesRewriter} for why the call site rather than the read site
 * is the thing that has to change.
 */
public class PaperClassloaderBytecodeModifier implements ClassloaderBytecodeModifier {

    @Override
    public byte[] modify(PluginMeta configuration, byte[] bytecode) {
        return MaterialValuesRewriter.rewrite(bytecode);
    }
}
