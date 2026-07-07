package com.memedaddy.ntmcombat.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NTMCombatTransformer implements IClassTransformer, Opcodes {

    // SRG field name -> MCP field name, keyed by class owner (internal format)
    private static final Map<String, Map<String, String>> FIELD_MAP = new HashMap<>();
    // SRG method name -> MCP method name, keyed by class owner (internal format)
    private static final Map<String, Map<String, String>> METHOD_MAP = new HashMap<>();
    // Method descriptor suffix filter (to avoid over-matching)
    private static final Map<String, String> METHOD_DESC = new HashMap<>();

    static {
        // DefaultVertexFormats field aliases (static, VertexFormat type)
        Map<String, String> dvfFields = new HashMap<>();
        dvfFields.put("field_181704_d", "PARTICLE_POSITION_TEX_COLOR_LMAP");
        dvfFields.put("field_181705_e", "POSITION");
        dvfFields.put("field_181706_f", "POSITION_COLOR");
        dvfFields.put("field_181707_g", "POSITION_TEX");
        dvfFields.put("field_181708_h", "POSITION_NORMAL");
        dvfFields.put("field_181709_i", "POSITION_TEX_COLOR");
        dvfFields.put("field_181710_j", "POSITION_TEX_NORMAL");
        dvfFields.put("field_181711_k", "POSITION_TEX_LMAP_COLOR");
        dvfFields.put("field_181712_l", "POSITION_TEX_COLOR_NORMAL");
        FIELD_MAP.put("net/minecraft/client/renderer/vertex/DefaultVertexFormats", dvfFields);

        // SPacketCustomPayload field aliases
        Map<String, String> spcpFields = new HashMap<>();
        spcpFields.put("field_149172_a", "channel");
        spcpFields.put("field_149171_b", "data");
        FIELD_MAP.put("net/minecraft/network/play/server/SPacketCustomPayload", spcpFields);

        // CPacketCustomPayload field aliases
        Map<String, String> cpcpFields = new HashMap<>();
        cpcpFields.put("field_149561_c", "data");
        FIELD_MAP.put("net/minecraft/network/play/client/CPacketCustomPayload", cpcpFields);

        // NetworkManager method aliases
        Map<String, String> nmMethods = new HashMap<>();
        nmMethods.put("func_150731_c", "isLocalChannel");
        METHOD_MAP.put("net/minecraft/network/NetworkManager", nmMethods);
        METHOD_DESC.put("func_150731_c", "()Z");

        // VertexFormat method aliases
        Map<String, String> vfMethods = new HashMap<>();
        vfMethods.put("func_177338_f", "getSize");
        vfMethods.put("func_181719_f", "getIntegerSize");
        vfMethods.put("func_181720_d", "getOffset");
        vfMethods.put("func_177340_e", "getColorOffset");
        vfMethods.put("func_177345_h", "getElementCount");
        vfMethods.put("func_177348_c", "getElement");
        vfMethods.put("func_177342_c", "getNormalOffset");
        vfMethods.put("func_177344_b", "getUvOffsetById");
        METHOD_MAP.put("net/minecraft/client/renderer/vertex/VertexFormat", vfMethods);
        METHOD_DESC.put("func_177338_f", "()I");
        METHOD_DESC.put("func_181719_f", "()I");
        METHOD_DESC.put("func_181720_d", "(I)I");
        METHOD_DESC.put("func_177340_e", "()I");
        METHOD_DESC.put("func_177345_h", "()I");
        METHOD_DESC.put("func_177348_c", "(I)Lnet/minecraft/client/renderer/vertex/VertexFormatElement;");
        METHOD_DESC.put("func_177342_c", "()I");
        METHOD_DESC.put("func_177344_b", "(I)I");

        // VertexFormatElement method aliases
        Map<String, String> vfeMethods = new HashMap<>();
        vfeMethods.put("func_177367_b", "getType");
        vfeMethods.put("func_177375_c", "getUsage");
        METHOD_MAP.put("net/minecraft/client/renderer/vertex/VertexFormatElement", vfeMethods);
        METHOD_DESC.put("func_177367_b", "()Lnet/minecraft/client/renderer/vertex/VertexFormatElement$EnumType;");
        METHOD_DESC.put("func_177375_c", "()Lnet/minecraft/client/renderer/vertex/VertexFormatElement$EnumUsage;");
    }

    // Set of NTM mixin class prefixes that need remapping
    private static final Set<String> NTM_MIXIN_PREFIXES = new HashSet<>();

    static {
        NTM_MIXIN_PREFIXES.add("com.hbm.mixin.");
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.startsWith("com.hbm.mixin.")) {
            try {
                ClassReader classReader = new ClassReader(basicClass);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classReader.accept(new SRGMappingVisitor(classWriter), 0);
                return classWriter.toByteArray();
            } catch (Exception e) {
                System.out.println("[NTMCombatTransformer] ERROR transforming " + transformedName + ": " + e);
                e.printStackTrace(System.out);
                return basicClass;
            }
        }

        return basicClass;
    }

    private static class SRGMappingVisitor extends ClassVisitor {

        public SRGMappingVisitor(ClassWriter classWriter) {
            super(Opcodes.ASM5, classWriter);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodCallRemapper(mv);
        }
    }

    private static class MethodCallRemapper extends MethodVisitor {

        public MethodCallRemapper(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            Map<String, String> fields = FIELD_MAP.get(owner);
            if (fields != null) {
                String mapped = fields.get(name);
                if (mapped != null) {
                    name = mapped;
                }
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            Map<String, String> methods = METHOD_MAP.get(owner);
            if (methods != null) {
                String mapped = methods.get(name);
                if (mapped != null) {
                    name = mapped;
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
