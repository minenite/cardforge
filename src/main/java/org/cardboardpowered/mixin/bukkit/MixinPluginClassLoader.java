package org.cardboardpowered.mixin.bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PluginClassLoader.class, remap = false)
public class MixinPluginClassLoader extends URLClassLoader {

	public MixinPluginClassLoader(URL[] urls) {
		super(urls);
		// TODO Auto-generated constructor stub
	}

	@Shadow(remap = false)
	private Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>();
    
	@Shadow(remap = false)
	private PluginDescriptionFile description;
    
	@Shadow(remap = false)
	private JarFile jar;
    
	@Shadow(remap = false)
	private Manifest manifest;
    
	@Shadow(remap = false)
	private URL url;
    
	@Shadow(remap = false)
	JavaPlugin plugin;
	
	
	// private int count = 0;
	
    @Overwrite(remap = false)
	protected Class<?> findClass(String name) throws ClassNotFoundException {
    	
    	/*
    	if (count < 100) {
    		System.out.println("FIND CLASS: " + name);
    		count += 1;
    	}
    	*/
    	
    	// Cardboard - Remap findClass
    	org.cardboardpowered.util.nms.RemapUtils remapUtils = (org.cardboardpowered.util.nms.RemapUtils) org.cardboardpowered.mohistremap.RemapUtilProvider.get();
    	if (remapUtils.needRemap(name.replace('/','.'))) {
        	org.cardboardpowered.mohistremap.ClassMapping remappedClassMapping = remapUtils.jarMapping.byNMSName.get(name);
            if(remappedClassMapping == null){
                throw new ClassNotFoundException(name.replace('/','.'));
            }
            return Class.forName( remappedClassMapping.getMcpName() );
        }
    	
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        // CardForge - let a plugin opt in to the NeoForge integration.
        // The plugin loader's parent does not reach CardForge's own module, so
        // without this a CardForge-native plugin gets NoClassDefFoundError for the
        // API it compiled against. Scoped to the published API and to NeoForge
        // itself; nothing else about plugin isolation changes, and a plugin that
        // never touches these packages is unaffected.
        if (name.startsWith("org.minenite.cardforge.api.")
                || name.startsWith("net.neoforged.")) {
            return Class.forName(name, false,
                    org.minenite.cardforge.api.CardForge.class.getClassLoader());
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            // Add details to zip file errors - help debug classloading
            JarEntry entry;
            try {
                entry = jar.getJarEntry(path);
            } catch (IllegalStateException zipFileClosed) {
                if (plugin == null) {
                    throw zipFileClosed;
                }
                throw new IllegalStateException("The plugin classloader for " + plugin.getName() + " has thrown a zip file error.", zipFileClosed);
            }

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    // classBytes = ByteStreams.toByteArray(is);
                    classBytes = remapUtils.getJarRemapper().remapClassFile(is, net.md_5.specialsource.repo.RuntimeRepo.getInstance()); // Cardboard
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                // CardForge - legacy plugins do not go through Paper's
                // ClassloaderBytecodeModifier, so apply the same rewrite here.
                // It has to run on the bytes as the plugin compiled them: Cardboard's
                // remapper below rewrites call owners, and after it has run the
                // invokestatic no longer matches org/bukkit/Material.values().
                classBytes = org.cardboardpowered.plugin.MaterialValuesRewriter.rewrite(classBytes);

                classBytes = org.bukkit.Bukkit.getServer().getUnsafe().processClass(description, path, classBytes); // Paper
                classBytes = remapUtils.remapFindClass(classBytes); // Cardboard - remapFindClass
                
                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if ( getPackage(pkgName) == null) {
                        try {
                            if (manifest != null) {
                                definePackage(pkgName, manifest, url);
                            } else {
                                definePackage(pkgName, null, null, null, null, null, null, null);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (getPackage(pkgName) == null) {
                                throw new IllegalStateException("Cannot find package " + pkgName);
                            }
                        }
                    }
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(url, signers);

                result = defineClass(name, classBytes, 0, classBytes.length, source);
            }

            if (result == null) {
                result = super.findClass(name);
            }

            classes.put(name, result);
            this.setClass(name, result); // Paper
        }

        return result;
    }
	
	@Shadow(remap = false)
	void setClass(@NotNull final String name, @NotNull final Class<?> clazz) {
        if (org.bukkit.configuration.serialization.ConfigurationSerializable.class.isAssignableFrom(clazz)) {
            Class<? extends org.bukkit.configuration.serialization.ConfigurationSerializable> serializable = clazz.asSubclass(org.bukkit.configuration.serialization.ConfigurationSerializable.class);
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(serializable);
        }
    }
	
}