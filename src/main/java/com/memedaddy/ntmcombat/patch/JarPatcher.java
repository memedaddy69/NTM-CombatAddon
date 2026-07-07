package com.memedaddy.ntmcombat.patch;

import org.objectweb.asm.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class JarPatcher {

    private static final Map<String, Map<String, String>> FIELD_MAP = new HashMap<>();
    private static final Map<String, Map<String, String>> METHOD_MAP = new HashMap<>();
    private static final Map<String, String> GLOBAL_FIELD_MAP = new HashMap<>();
    private static final Map<String, String> GLOBAL_METHOD_MAP = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: JarPatcher <srg-mcp.srg> <input.jar> <output.jar>");
            System.exit(1);
        }
        File srgFile = new File(args[0]);
        File input = new File(args[1]);
        File output = new File(args[2]);
        if (!srgFile.exists()) {
            System.err.println("SRG mappings file not found: " + srgFile);
            System.exit(1);
        }
        if (!input.exists()) {
            System.err.println("Input jar not found: " + input);
            System.exit(1);
        }
        loadMappings(srgFile);
        patchJar(input, output);
    }

    static void loadMappings(File srgFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(srgFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("FD: ")) {
                    parseFieldLine(line.substring(4));
                } else if (line.startsWith("MD: ")) {
                    parseMethodLine(line.substring(4));
                }
            }
        }
        // Build global fallback maps
        for (Map<String, String> fields : FIELD_MAP.values()) {
            GLOBAL_FIELD_MAP.putAll(fields);
        }
        for (Map<String, String> methods : METHOD_MAP.values()) {
            GLOBAL_METHOD_MAP.putAll(methods);
        }
        System.out.println("[JarPatcher] Loaded " + GLOBAL_FIELD_MAP.size() + " field and "
            + GLOBAL_METHOD_MAP.size() + " method SRG mappings");
    }

    private static void parseFieldLine(String line) {
        // Format: owner/srgName owner/mcpName
        int space = line.indexOf(' ');
        if (space < 0) return;
        String left = line.substring(0, space);
        String right = line.substring(space + 1);
        int leftSlash = left.lastIndexOf('/');
        int rightSlash = right.lastIndexOf('/');
        if (leftSlash < 0 || rightSlash < 0) return;
        String owner = left.substring(0, leftSlash);
        String srgName = left.substring(leftSlash + 1);
        String mcpName = right.substring(rightSlash + 1);
        if (srgName.startsWith("field_") || srgName.startsWith("func_")) {
            FIELD_MAP.computeIfAbsent(owner, k -> new HashMap<>()).put(srgName, mcpName);
        }
    }

    private static void parseMethodLine(String line) {
        // Format: owner/srgName (desc) owner/mcpName (desc)
        // Find the descriptor end after the first method reference
        int firstParen = line.indexOf('(');
        if (firstParen < 0) return;
        int closeParen = line.indexOf(')', firstParen);
        if (closeParen < 0) return;
        // After the first descriptor, find the owner/mcpName (desc) part
        int afterFirstDesc = closeParen + 1;
        // Skip spaces
        while (afterFirstDesc < line.length() && line.charAt(afterFirstDesc) == ' ') afterFirstDesc++;
        int secondParen = line.indexOf('(', afterFirstDesc);
        if (secondParen < 0) return;
        // The mcpName part is from afterFirstDesc to secondParen (trimmed)
        String mcpPart = line.substring(afterFirstDesc, secondParen).trim();
        int mcpSlash = mcpPart.lastIndexOf('/');
        if (mcpSlash < 0) return;
        String mcpName = mcpPart.substring(mcpSlash + 1);

        // The left part is owner/srgName before the first paren
        String leftPart = line.substring(0, firstParen).trim();
        int leftSlash = leftPart.lastIndexOf('/');
        if (leftSlash < 0) return;
        String owner = leftPart.substring(0, leftSlash);
        String srgName = leftPart.substring(leftSlash + 1);
        if (srgName.startsWith("field_") || srgName.startsWith("func_")) {
            METHOD_MAP.computeIfAbsent(owner, k -> new HashMap<>()).put(srgName, mcpName);
        }
    }

    public static void patchJar(File input, File output) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        Manifest manifest = null;
        try (JarFile jf = new JarFile(input)) {
            manifest = jf.getManifest();
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                byte[] data = readAll(jf.getInputStream(entry));
                String name = entry.getName();
                if (name.endsWith(".class") && name.startsWith("com/hbm/")) {
                    data = patchClass(name, data);
                }
                entries.put(name, data);
            }
        }
        JarOutputStream jos;
        if (manifest != null) {
            jos = new JarOutputStream(new FileOutputStream(output), manifest);
        } else {
            jos = new JarOutputStream(new FileOutputStream(output));
        }
        try {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                String name = e.getKey();
                if (name.equals("META-INF/MANIFEST.MF") || name.equals("META-INF/")
                    || name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC")) {
                    continue;
                }
                JarEntry je = new JarEntry(name);
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        } finally {
            jos.close();
        }
    }

    private static byte[] patchClass(String className, byte[] data) {
        try {
            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cr.accept(new RemappingVisitor(cw), 0);
            return cw.toByteArray();
        } catch (Exception e) {
            System.err.println("[JarPatcher] ERROR patching " + className + ": " + e);
            return data;
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static final Set<String> MC_PREFIXES = new HashSet<>(Arrays.asList(
        "net/minecraft/", "net/minecraftforge/", "com/mojang/"
    ));

    private static boolean isMcClass(String owner) {
        for (String prefix : MC_PREFIXES) {
            if (owner.startsWith(prefix)) return true;
        }
        return false;
    }

    private static String resolveFieldName(String owner, String srgName) {
        if (!isMcClass(owner)) return srgName;
        Map<String, String> fields = FIELD_MAP.get(owner);
        if (fields != null && fields.containsKey(srgName)) {
            return fields.get(srgName);
        }
        String mcp = GLOBAL_FIELD_MAP.get(srgName);
        return mcp != null ? mcp : srgName;
    }

    private static String resolveMethodName(String owner, String srgName) {
        if (!isMcClass(owner)) return srgName;
        Map<String, String> methods = METHOD_MAP.get(owner);
        if (methods != null && methods.containsKey(srgName)) {
            return methods.get(srgName);
        }
        String mcp = GLOBAL_METHOD_MAP.get(srgName);
        return mcp != null ? mcp : srgName;
    }

    private static class RemappingVisitor extends ClassVisitor {
        RemappingVisitor(ClassWriter cw) { super(Opcodes.ASM5, cw); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
            return new MethodVisitor(Opcodes.ASM5, mv) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    super.visitFieldInsn(opcode, owner, resolveFieldName(owner, name), desc);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    super.visitMethodInsn(opcode, owner, resolveMethodName(owner, name), desc, itf);
                }
            };
        }
    }
}
