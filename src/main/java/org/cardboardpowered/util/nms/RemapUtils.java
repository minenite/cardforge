package org.cardboardpowered.util.nms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.cardboardpowered.CardboardConfig;
import org.cardboardpowered.CardboardLogger;
import org.cardboardpowered.mohistremap.ClassMapping;
import org.cardboardpowered.mohistremap.IRemapUtils;
import org.cardboardpowered.mohistremap.utils.ASMUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mohistmc.remap.remappers.BannerInheritanceMap;
import com.mohistmc.remap.remappers.BannerInheritanceProvider;
import com.mohistmc.remap.remappers.BannerJarMapping;
import com.mohistmc.remap.remappers.BannerJarRemapper;
import com.mohistmc.remap.remappers.BannerSuperClassRemapper;
import com.mohistmc.remap.remappers.ClassRemapperSupplier;
import com.mohistmc.remap.remappers.ReflectMethodRemapper;
import com.mohistmc.remap.remappers.ReflectRemapper;

import net.md_5.specialsource.InheritanceMap;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;

/**
 * @author pyz
 * @date 2019/6/30 11:50 PM
 */
public class RemapUtils implements IRemapUtils {
	
	private static CardboardLogger LOGGER = CardboardLogger.get("RemapUtils");

    public static BannerJarMapping jarMapping;
    public static BannerJarRemapper jarRemapper;
    private static final List<Remapper> remappers = new ArrayList<>();
    public static InheritanceMap inheritanceMap;
    
    public static boolean DEBUG_VERBOSE_CALLS = true;

    // public static String NMS_VERSION = "v1_21_R7"; // "v1_21_R3"; // "v1_20_R4";

    @Override
    public void init() {
    	LOGGER.debug("Remap Util init");

    	inheritanceMap = new CbInheritanceMap();

        jarMapping = new BannerJarMapping();
        // v1_20_R1
        // jarMapping.packages.put("org/bukkit/craftbukkit/" + NMS_VERSION + "/", "org/bukkit/craftbukkit/");
        // jarMapping.packages.put("org/bukkit/craftbukkit/" + NMS_VERSION, "org/bukkit/craftbukkit");
        jarMapping.packages.put("org/bukkit/craftbukkit/libs/it/unimi/dsi/fastutil/", "it/unimi/dsi/fastutil/");
        jarMapping.packages.put("org/bukkit/craftbukkit/libs/jline/", "jline/");
        jarMapping.packages.put("org/bukkit/craftbukkit/libs/org/apache/commons/", "org/apache/commons/");
        jarMapping.packages.put("org/bukkit/craftbukkit/libs/org/objectweb/asm/", "org/objectweb/asm/");

        jarMapping.classes.put("org/spigotmc/event/entity/EntityMountEvent", "org/bukkit/event/entity/EntityMountEvent");
        jarMapping.classes.put("org/spigotmc/event/entity/EntityDismountEvent", "org/bukkit/event/entity/EntityDismountEvent");

        jarMapping.setInheritanceMap(new BannerInheritanceMap());
        jarMapping.setFallbackInheritanceProvider(new BannerInheritanceProvider());

        try {
            jarMapping.loadMappings(
                    new BufferedReader(new InputStreamReader(RemapUtils.class.getClassLoader()
                            // .getResourceAsStream("mappings/spigot2srg-1.20.srg"))),
                    		.getResourceAsStream("mappings/cardboard.srg"))),
                    null,
                    null, false);
        } catch (Exception e) {
        	System.out.println("debug: error loading remaputils");
            e.printStackTrace();
        }

        File dir = new File("mappings");
        dir.mkdirs();

        BiMap<String, String> inverseClassMap = HashBiMap.create(jarMapping.classes).inverse();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(RemapUtils.class.getClassLoader().getResourceAsStream("mappings/inheritanceMap.txt")))) {
            inheritanceMap.load(reader, inverseClassMap);
        } catch (IOException e1) {
			e1.printStackTrace();
		}

        jarMapping.classes.put("org/bukkit/craftbukkit/CraftWorld", "org/cardboardpowered/impl/world/CraftWorld");

        JointProvider provider = new JointProvider();
        provider.add(inheritanceMap);
        provider.add(new ClassLoaderProvider(ClassLoader.getSystemClassLoader()));
        provider.add(new BannerInheritanceProvider());
        jarMapping.setFallbackInheritanceProvider(provider);
        jarRemapper = new BannerJarRemapper(jarMapping);
        remappers.add(jarRemapper);
        remappers.add(new ReflectRemapper());
        jarMapping.initFastMethodMapping(jarRemapper);
        ReflectMethodRemapper.init();

        try {
            Class.forName("org.cardboardpowered.mohistremap.proxy.ProxyMethodHandlesLookup");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        DEBUG_VERBOSE_CALLS = CardboardConfig.DEBUG_VERBOSE_CALLS;
    }

    @Override
    public byte[] remapFindClass(byte[] bs) {
        ClassReader reader = new ClassReader(bs); // Turn from bytes into visitor
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        for (Remapper remapper : remappers) {

            ClassNode container = new ClassNode();
            ClassRemapper classRemapper;
            if (remapper instanceof ClassRemapperSupplier) {
                classRemapper = ((ClassRemapperSupplier) remapper).getClassRemapper(container);
            } else {
                classRemapper = new ClassRemapper(container, remapper);
            }
            classNode.accept(classRemapper);
            classNode = container;
        }
        BannerSuperClassRemapper.init(classNode);
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();

    }

    // 26.2 ships unobfuscated on both platforms, so no mapping resolver is needed.
    // Names are already the ones plugins reflect against.
    
    @Override
    public String map(String typeName) {
    	
    	// Check if typeName is not in internal class name format
    	boolean isRequestNotInternalName = typeName.indexOf('.') != -1 && typeName.indexOf('/') == -1;

        typeName = mapPackage(typeName);
        String res = jarMapping.classes.getOrDefault(typeName, typeName);

        if (isRequestNotInternalName) {
        	res = res.replace('/', '.');
    	}
        
        return res; // jarMapping.classes.getOrDefault(typeName, typeName);
    }

    @Override
    public String reverseMap(String typeName) {
        ClassMapping mapping = jarMapping.byNMSInternalName.get(typeName);
        return mapping == null ? typeName : mapping.getNmsSrcName();
    }

    @Override
    public String reverseMap(Class<?> clazz) {
        ClassMapping mapping = jarMapping.byMCPName.get(clazz.getName());
        return mapping == null ? ASMUtils.toInternalName(clazz) : mapping.getNmsSrcName();
    }
    
    public String reverseMap_name(String class_name) {
        ClassMapping mapping = jarMapping.byMCPName.get(class_name);
        return mapping == null ? ASMUtils.toInternalName(class_name) : mapping.getNmsSrcName();
    }

    @Override
    public String mapPackage(String typeName) {
        for (Map.Entry<String, String> entry : jarMapping.packages.entrySet()) {
            String prefix = entry.getKey();
            if (typeName.startsWith(prefix)) {
                return entry.getValue() + typeName.substring(prefix.length());
            }
        }
        return typeName;
    }

    @Override
    public String remapMethodDesc(String methodDescriptor) {
        Type rt = Type.getReturnType(methodDescriptor);
        Type[] ts = Type.getArgumentTypes(methodDescriptor);
        rt = Type.getType(ASMUtils.toDescriptorV2(map(ASMUtils.getInternalName(rt))));
        for (int i = 0; i < ts.length; i++) {
            ts[i] = Type.getType(ASMUtils.toDescriptorV2(map(ASMUtils.getInternalName(ts[i]))));
        }
        return Type.getMethodType(rt, ts).getDescriptor();
    }

    @Override
    public String mapMethodName(Class<?> clazz, String name, MethodType methodType) {
        return mapMethodName(clazz, name, methodType.parameterArray());
    }

    @Override
    public String mapMethodName(Class<?> type, String name, Class<?>... parameterTypes) {
    	String mm = "";
    	for (Class<?> pt : parameterTypes) {
    		mm += ", " + pt.getName();
    	}
    	mm += ")";
    	
    	if (mm.length() > 2) {
    		mm = mm.substring(2);
    	}
    	mm = "(" + mm;
    	
    	// Failed to find method public (org.bukkit.World) ??org.bukkit.craftbukkit.CraftWorld?? getWorld();
    	if (name.contains("getWorld")) {
    		System.out.println(type + " / " + name + " / " + mm);
    	}
    	
    	String res = jarMapping.fastMapMethodName(type, name, parameterTypes);

        return res;
    }

    @Override
    public String inverseMapMethodName(Class<?> type, String name, Class<?>... parameterTypes) {
        return jarMapping.fastReverseMapMethodName(type, name, parameterTypes);
    }

    @Override
    public String mapFieldName(Class<?> type, String fieldName) {
    	if (DEBUG_VERBOSE_CALLS) {
    		System.out.println("Reflection: " + type.getName() + " / " + fieldName);
    	}
    	
    	String revType = reverseMap(type);
        String key = revType + "/" + fieldName;

        String mapped = jarMapping.fields.get(key);
        if (mapped == null) {
            Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                mapped = mapFieldName(superClass, fieldName);
            }
        }

        if (DEBUG_VERBOSE_CALLS) {
        	System.out.println("DEBUG: FIELD: " + revType + "/" + type.getName() + " / " + fieldName + " = " + (mapped != null ? mapped : fieldName));
        }

        return mapped != null ? mapped : fieldName;
    }

    @Override
    public String inverseMapFieldName(Class<?> type, String fieldName) {
        return jarMapping.fastReverseMapFieldName(type, fieldName);
    }

    @Override
    public String inverseMapName(Class<?> clazz) {
        ClassMapping mapping = jarMapping.byMCPName.get(clazz.getName());
        return mapping == null ? clazz.getName() : mapping.getNmsName();
    }

    @Override
    public String inverseMapSimpleName(Class<?> clazz) {
        ClassMapping mapping = jarMapping.byMCPName.get(clazz.getName());
        return mapping == null ? clazz.getSimpleName() : mapping.getNmsSimpleName();
    }

    @Override
    public String getClassDescriptor(Class<?> clazz, String name) {
        if (clazz.isArray()) {
            return "[" + getClassDescriptor(clazz.getComponentType(), name);
        } else if (clazz.isPrimitive()) {
            if (clazz == void.class) return "V";
            if (clazz == boolean.class) return "Z";
            if (clazz == byte.class) return "B";
            if (clazz == char.class) return "C";
            if (clazz == short.class) return "S";
            if (clazz == int.class) return "I";
            if (clazz == long.class) return "J";
            if (clazz == float.class) return "F";
            if (clazz == double.class) return "D";
        } else {
        	
        	if (!name.isEmpty()) {
        		// String in = jt.getInternalName().replace('/', '.');
    			// 26.2 is unobfuscated: the runtime name is already the mapped name.
    			return "L" + clazz.getName().replace('.', '/') + ";";
        	}
        	
            return "L" + clazz.getName().replace('.', '/') + ";";
        }
        throw new IllegalArgumentException("Unsupported class: " + clazz);
    }

	@Override
	public String getClassDescriptorResolveName(String namespace, String name) {
		// 26.2 is unobfuscated; nothing to unmap.
		return name;
	}

	@Override
	public BannerJarRemapper getJarRemapper() {
		return jarRemapper;
	}


	@Override
	public boolean shouldExtraDebugLog() {
		return CardboardConfig.DEBUG_VERBOSE_CALLS;
	}
}
