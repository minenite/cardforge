/**
 * Cardboard - Bukkit/Spigot/Paper API for Fabric
 * Copyright (C) 2023-2026, CardboardPowered.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.cardboardpowered.util.nms;

import org.bukkit.Material;
import org.bukkit.craftbukkit.util.Commodore;
import org.cardboardpowered.CardboardConfig;
import org.cardboardpowered.impl.util.CardboardMagicNumbers;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.cardboardpowered.CardboardMod;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ReflectionMethodVisitor extends MethodVisitor {

    public static ArrayList<String> SKIP = new ArrayList<>();
    static {
        SKIP.add("vault");
        SKIP.add("worldguard");
    }

    private String pln;
    // private MappingResolver mr;

    public ReflectionMethodVisitor(int api, MethodVisitor visitMethod, String pln) {
        super(api, visitMethod);
        this.pln = pln;

        // this.mr = FabricLoader.getInstance().getMappingResolver();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (CardboardConfig.DEBUG_VERBOSE_CALLS) {
        	if (!owner.startsWith("java/")) {
        		CardboardMod.LOGGER.info(owner + " / " + name);
        	}
        }
    	
    	if (owner.equalsIgnoreCase("org/bukkit/Material")) {
            if (CardboardMagicNumbers.MODDED_MATERIALS.containsKey(name)) {
				System.out.println("Modded Material Debug: " + name);
                super.visitFieldInsn( opcode, owner, "STONE", desc );
                return;
            }
        }

    	if (name.equalsIgnoreCase("field_41255")) {
    		// Note: Find out why this is being mapped wrong in the worldedit adaptor
    		name = "field_41254";
    	}

    	if (name.equalsIgnoreCase("field_41199")) {
    		// Note: Find out why this is being mapped wrong in the worldedit adaptor
    		name = "field_41197";
    	}

        super.visitFieldInsn( opcode, owner, name, desc );
    }

    public static Field Material_getField(String name) throws NoSuchFieldException, SecurityException {
        try {
            return Material.class.getField(name);
        } catch (NoSuchFieldException | SecurityException e) {
			System.out.println("STONE:? " + e.getMessage());
            return Material.class.getField("STONE");
        }
    }

    @Deprecated(since = "26.1")
    private static String do_map(String owner, String name, String desc) {
    	return name;
    }
    
    @Deprecated(since = "26.1")
    private static String find_in_inheritance(Class<?> clazz, String obf_name, String desc, String sigg) {
    	return obf_name;
    }
    
    @Deprecated(since = "26.1")
    private static String find_in_inheritance_f(Class<?> clazz, String obf_name, String desc, String sigg) {
    	return obf_name;
    }

    private void debug(String o) {
    	if (CardboardConfig.DEBUG_VERBOSE_CALLS) {
    		CardboardMod.LOGGER.info(o);
    	}
    }
    
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (CardboardConfig.DEBUG_VERBOSE_CALLS) {
        	//if (!owner.startsWith("java/")) {
        		CardboardMod.LOGGER.info(owner + " / " + name);
        	//}
        }

		// Redirect WorldGuard Custom Logger (JUL->SLF4J)
        if (owner.contains("com/sk89q/worldguard/util/logging/RecordMessagePrefixer") ) {
        	owner = owner.replace("com/sk89q/worldguard/util/logging/RecordMessagePrefixer", "org/cardboardpowered/util/RecordMessagePrefixer");
        }

        if (owner.contains("LegacyPotionMetaProvider")) {
        	debug(owner + " " + name + " " + desc);
        	owner = owner.replace("LegacyPotionMetaProvider", "ModernPotionMetaProvider");
        }

        if (owner.startsWith("org/bukkit/craftbukkit") && owner.contains(ReflectionRemapper.NMS_VERSION)) {
        	System.out.println("Stripping version package (" + ReflectionRemapper.NMS_VERSION + ") from org/bukkit/craftbukkit reference.");
        	owner = owner.replace("org/bukkit/craftbukkit/" + ReflectionRemapper.NMS_VERSION + "/", "org/bukkit/craftbukkit/");
        }
        
        if (owner.startsWith("net/minecraft") && name.equals("getMinecraftServer")) {
            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getNmsServer", desc, false );
            return;
        }

        if (owner.startsWith("net/minecraft") && owner.contains("MinecraftServer") && name.equals("getServer")) {
            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getNmsServer", desc, false );
            return;
        }
        
        if (owner.startsWith("net/minecraft") && (owner.contains("DedicatedServer") || owner.contains("class_3176")) && name.equals("getServer")) {
            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getNmsServer", desc, false );
            return;
        }
        
        if (name.contains("method_45136")) {
        	if (opcode == Opcodes.INVOKESTATIC) {
        		// Give us the static method
        		name = "method_12829";
        	}
        }

        if (owner.equalsIgnoreCase("org/bukkit/Material")) {
            if (name.equalsIgnoreCase("getField")) {
                // System.out.println("\nGET MATERIAL FIELD!!!!!\n");
                super.visitFieldInsn( opcode, "org/cardboardpowered/util/nms/ReflectionMethodVisitor", "Material_getField", desc );
                return;
            }
        }

        if (owner.equalsIgnoreCase("com/comphenix/protocol/utility/MinecraftReflection")) {
            // System.out.println("PROTOCOLLIB REFLECTION: " + name);
            if (name.equals("getCraftBukkitClass") || name.equals("getMinecraftClass")) {
                super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ProtocolLibMapper", name, desc, false );
                return;
            }
        }

        if (owner.equalsIgnoreCase("com/comphenix/protocol/injector/netty/ChannelInjector")) {
            if (name.equals("guessCompression")) {
                super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ProtocolLibMapper", name, desc, false );
                return;
            }
        }
        
        if (owner.equalsIgnoreCase("com/sk89q/worldguard/bukkit/util/Materials")) {
            if (name.equals("isSpawnEgg") || name.equals("getEntitySpawnEgg") || name.equals("isArmor") ||
                    name.equals("isToolApplicable") || name.equals("isWaxedCopper")) {
                super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/WorldGuardMaterialHelper", name, desc, false );
                return;
            }
        }
        
        /*
        if (owner.startsWith("net/minecraft") && name.startsWith("method_")) {
        	String namespace = mr.getCurrentRuntimeNamespace();
        	if (namespace.equalsIgnoreCase("named")) {
        		name = mr.mapMethodName("intermediary", owner.replace('/', '.'), name, desc);
        	}
        }
        */

        for (String str : SKIP) {
            if (this.pln.equalsIgnoreCase(str) || owner.startsWith("org/bukkit")) {
                // Skip Vault cause weird things happen
                super.visitMethodInsn( opcode, owner, name, desc, itf );
                return;
            }
        }

        if (owner.equalsIgnoreCase("java/lang/Class") && name.equalsIgnoreCase("forName") && desc.equalsIgnoreCase("(Ljava/lang/String;)Ljava/lang/Class;"))
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "mapClassName", "(Ljava/lang/String;)Ljava/lang/String;", false);
        
        if (owner.equalsIgnoreCase("java/lang/Class") && name.equalsIgnoreCase("getMethods")) {
            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getMethods", "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;", false );
            return;
        }

        if (owner.startsWith("net/minecraft")) {
            if (owner.equalsIgnoreCase("net/minecraft/server/dedicated/DedicatedServer") && name.equalsIgnoreCase("getVersion")) {
                // Add MinecraftServer#getVersion
                super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getMinecraftServerVersion", "()Ljava/lang/String;", false);
                return;
            }
        }
        
        /**
         * 
         * A class member of net.minecraft.world.level.Level was not found!
[23:24:49] [Server thread/WARN]: Failed to find method public (org.bukkit.World) ??org.bukkit.craftbukkit.CraftWorld?? getWorld(); - Alternatives:
[23:24:49] [Server thread/WARN]:   - public abstract border.WorldBorder getWorldBorder();
[23:24:49] [Server thread/WARN]:   - public org.cardboardpowered.impl.world.CraftWorld cardboard$getWorld();
[23:24:49] [Server thread/WARN]:   - public net.minecraft.server.MinecraftServer getServer();
[23:24:49] [Server thread/WARN]:   - public Thread getThread();
[23:24:49] [Server thread/WARN]:   - public abstract net.minecraft.world.scores.Scoreboard getScoreboard();
[23:24:49] [Server thread/WARN]:   - public net.minecraft.server.level.ServerLevel getMinecraftWorld();
[23:24:49] [Server thread/WARN]:   - public net.minecraft.util.RandomSource getRandom();
[23:24:49] [Server thread/WARN]:   - public storage.LevelData getLevelData();
         */
        
        if (owner.startsWith("net/minecraft/world/level/Level")) {
        	
        	if (desc.contains("org/bukkit/")) {
        		System.out.println("LEVEL METH: " + name + "  " + desc);
        	}
        	
        	if (name.equalsIgnoreCase("getWorld") && desc.contains("CraftWorld")) {
        		name = "cardboard$getWorld";
        	}
        	if (name.equalsIgnoreCase("getWorld") && desc.contains("org/bukkit/")) {
        		name = "cardboard$getWorld";
        	}
        	if (name.equalsIgnoreCase("getServer") && desc.contains("org/bukkit/")) {
                super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/cardboardpowered/util/nms/ReflectionRemapper", "getCraftServer", "()Lorg/bukkit/craftbukkit/CraftServer;", false);
                return;
        	}
        }
        
        
        
        owner = Commodore.getOriginalOrRewrite(owner);

        super.visitMethodInsn( opcode, owner, name, desc, itf );
    }

}
