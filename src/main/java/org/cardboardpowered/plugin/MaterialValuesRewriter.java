package org.cardboardpowered.plugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites {@code Material.values()} calls in plugin classes to CardForge's
 * dynamic bridge, so plugins see NeoForge-added materials.
 *
 * <p>Modded materials are added to Material by writing its private static final
 * {@code $VALUES} array through Unsafe. That write is real, but
 * {@code Material.values()} reads a static final field that HotSpot has already
 * constant-folded by the time any plugin runs, so it keeps returning the
 * pre-extension array. Patching the read site does not help - the fold happened
 * first. Redirecting the call site does.
 *
 * <p>The substituted method has an identical descriptor,
 * {@code ()[Lorg/bukkit/Material;}, so this is a drop-in replacement: a
 * precompiled plugin jar works unchanged, with no source changes and no
 * recompilation.
 *
 * <p>Only {@code invokestatic org/bukkit/Material.values()} is touched. Every
 * other Material member - {@code valueOf}, {@code getMaterial}, the registries,
 * field access - is left exactly as the plugin compiled it, so ordinary Bukkit
 * lookup behaviour is unchanged.
 *
 * <p>Note this covers the direct call only. A plugin reaching the same array by
 * reflection, or through {@code EnumSet.allOf(Material.class)} /
 * {@code Material.class.getEnumConstants()}, still gets the folded constant,
 * because those read the JDK's own cached copy rather than calling
 * {@code values()}. See docs/COMPATIBILITY.md.
 */
public final class MaterialValuesRewriter {

    private static final String MATERIAL = "org/bukkit/Material";
    private static final String BRIDGE = "org/cardboardpowered/api/CardForgeMaterials";
    private static final String VALUES = "values";
    private static final String DESCRIPTOR = "()[Lorg/bukkit/Material;";

    private MaterialValuesRewriter() {
    }

    /**
     * Returns the class bytes with any {@code Material.values()} call redirected.
     *
     * <p>Never throws: a plugin that fails to transform is returned untouched
     * rather than failing to load, since the only consequence is the pre-existing
     * behaviour of not seeing modded materials.
     *
     * @param bytecode the class as read from the plugin jar
     * @return the rewritten class, or the input unchanged if nothing matched
     */
    public static byte[] rewrite(byte[] bytecode) {
        try {
            ClassReader reader = new ClassReader(bytecode);
            ClassWriter writer = new ClassWriter(reader, 0);
            Rewriter rewriter = new Rewriter(writer);
            reader.accept(rewriter, 0);
            return rewriter.rewrote ? writer.toByteArray() : bytecode;
        } catch (Throwable t) {
            // Returning the class untouched only costs the plugin its view of
            // modded materials, so this must not stop it loading - but it is not
            // swallowed either, since nothing else would reveal it.
            org.cardboardpowered.CardboardMod.LOGGER.warning(
                    "Could not rewrite Material.values() in a plugin class: " + t);
            return bytecode;
        }
    }

    private static final class Rewriter extends ClassVisitor {

        private boolean rewrote;

        Rewriter(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (delegate == null) {
                return null;
            }
            return new MethodVisitor(Opcodes.ASM9, delegate) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName,
                                            String methodDescriptor, boolean isInterface) {
                    if (opcode == Opcodes.INVOKESTATIC
                            && MATERIAL.equals(owner)
                            && VALUES.equals(methodName)
                            && DESCRIPTOR.equals(methodDescriptor)
                            && !isInterface) {
                        Rewriter.this.rewrote = true;
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, VALUES, DESCRIPTOR, false);
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                }
            };
        }
    }
}
