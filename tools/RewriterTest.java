import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.cardboardpowered.plugin.MaterialValuesRewriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Regression test for the Material.values() call-site rewrite.
 *
 * Runs against a real, precompiled plugin jar - the same bytes the server would
 * load - and checks two things that have to hold together:
 *
 *   1. every invokestatic org/bukkit/Material.values() becomes a call to
 *      CardForgeMaterials.values(), with the descriptor unchanged, so the plugin
 *      needs no source change and no recompilation;
 *   2. every other Material reference the plugin made is left exactly as it was,
 *      so ordinary lookup and registry behaviour cannot have been disturbed.
 *
 * Usage: RewriterTest <plugin.jar>
 */
public final class RewriterTest {

    record Call(String owner, String name, String descriptor) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: RewriterTest <plugin.jar>");
            System.exit(2);
        }

        Path jarPath = Path.of(args[0]);
        int classes = 0;
        int failures = 0;
        boolean sawValuesCall = false;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (var entry : jar.stream().toList()) {
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                classes++;

                byte[] original;
                try (InputStream in = jar.getInputStream(entry)) {
                    original = in.readAllBytes();
                }
                byte[] rewritten = MaterialValuesRewriter.rewrite(original);

                List<Call> before = calls(original);
                List<Call> after = calls(rewritten);

                List<Call> beforeValues = before.stream()
                        .filter(c -> c.owner().equals("org/bukkit/Material") && c.name().equals("values"))
                        .toList();
                List<Call> afterValues = after.stream()
                        .filter(c -> c.owner().equals("org/bukkit/Material") && c.name().equals("values"))
                        .toList();
                List<Call> bridged = after.stream()
                        .filter(c -> c.owner().equals("org/cardboardpowered/api/CardForgeMaterials"))
                        .toList();

                if (!beforeValues.isEmpty()) {
                    sawValuesCall = true;

                    if (!afterValues.isEmpty()) {
                        System.out.println("FAIL " + entry.getName()
                                + ": " + afterValues.size() + " Material.values() call(s) left unrewritten");
                        failures++;
                    }
                    if (bridged.size() != beforeValues.size()) {
                        System.out.println("FAIL " + entry.getName() + ": expected " + beforeValues.size()
                                + " bridge call(s), found " + bridged.size());
                        failures++;
                    }
                    for (Call c : bridged) {
                        if (!c.descriptor().equals("()[Lorg/bukkit/Material;") || !c.name().equals("values")) {
                            System.out.println("FAIL " + entry.getName()
                                    + ": bridge call has wrong shape: " + c);
                            failures++;
                        }
                    }
                    if (failures == 0) {
                        System.out.println("ok   " + entry.getName() + ": rewrote "
                                + beforeValues.size() + " Material.values() call(s), descriptor preserved");
                    }
                }

                // Everything that is not Material.values() must be untouched.
                List<Call> otherBefore = before.stream().filter(c -> !isValues(c)).toList();
                List<Call> otherAfter = after.stream()
                        .filter(c -> !c.owner().equals("org/cardboardpowered/api/CardForgeMaterials"))
                        .toList();
                if (!otherBefore.equals(otherAfter)) {
                    System.out.println("FAIL " + entry.getName()
                            + ": non-values() calls changed (" + otherBefore.size()
                            + " before, " + otherAfter.size() + " after)");
                    failures++;
                }

                List<Call> materialBefore = otherBefore.stream()
                        .filter(c -> c.owner().equals("org/bukkit/Material")).toList();
                if (!materialBefore.isEmpty() && failures == 0) {
                    System.out.println("ok   " + entry.getName() + ": "
                            + materialBefore.size() + " other Material call(s) preserved");
                }
            }
        }

        System.out.println();
        System.out.println("scanned " + classes + " class(es) in " + jarPath.getFileName());
        if (!sawValuesCall) {
            System.out.println("FAIL: the jar contains no Material.values() call, so this proves nothing");
            System.exit(1);
        }
        if (failures > 0) {
            System.out.println(failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("PASS");
    }

    private static boolean isValues(Call c) {
        return c.owner().equals("org/bukkit/Material")
                && c.name().equals("values")
                && c.descriptor().equals("()[Lorg/bukkit/Material;");
    }

    private static List<Call> calls(byte[] bytecode) {
        List<Call> found = new ArrayList<>();
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        found.add(new Call(owner, methodName, methodDescriptor));
                    }
                };
            }
        }, 0);
        return found;
    }
}
