package com.memedaddy.ntmcombat.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SRPMultipartUtil {

    private static final String SRP_ENTITY_BODY_CLASS = "com.dhanantry.scapeandrunparasites.entity.EntityBody";
    private static final String SRP_PARASITE_BASE_CLASS = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase";
    private static final String SRP_BODY_PARTS_INTERFACE = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityBodyParts";

    private static boolean srpPresent = false;
    private static Class<?> clazzEntityBody;
    private static Class<?> clazzParasiteBase;
    private static Class<?> clazzBodyParts;
    private static Field partIdField;
    private static Method getFatherMethod;
    private static Method attackEntityBodyFromMethod;

    public enum BodyPartType { MAIN, LIMB, TENDRIL, UNKNOWN }

    static {
        try {
            clazzEntityBody = Class.forName(SRP_ENTITY_BODY_CLASS);
            clazzParasiteBase = Class.forName(SRP_PARASITE_BASE_CLASS);
            clazzBodyParts = Class.forName(SRP_BODY_PARTS_INTERFACE);

            getFatherMethod = clazzEntityBody.getDeclaredMethod("getFather");
            getFatherMethod.setAccessible(true);

            partIdField = clazzEntityBody.getDeclaredField("partId");
            partIdField.setAccessible(true);

            attackEntityBodyFromMethod = clazzBodyParts.getDeclaredMethod(
                    "attackEntityBodyFrom", DamageSource.class, float.class, int.class, boolean.class
            );

            srpPresent = true;
        } catch (Exception e) {
            srpPresent = false;
        }
    }

    public static boolean isActive() {
        return srpPresent;
    }

    public static boolean isEntityBody(Entity entity) {
        return srpPresent && clazzEntityBody.isInstance(entity);
    }

    public static Entity getParent(Entity entity) {
        if (!srpPresent || !clazzEntityBody.isInstance(entity)) return null;
        try { return (Entity) getFatherMethod.invoke(entity); }
        catch (Exception e) { return null; }
    }

    public static int getPartId(Entity entity) {
        if (!srpPresent || !clazzEntityBody.isInstance(entity)) return -1;
        try { return partIdField.getInt(entity); }
        catch (Exception e) { return -1; }
    }

    public static BodyPartType getBodyPartType(int partId) {
        if (partId >= 0 && partId <= 9) return BodyPartType.MAIN;
        if (partId >= 20 && partId <= 29) return BodyPartType.LIMB;
        if (partId >= 40 && partId <= 49) return BodyPartType.TENDRIL;
        return BodyPartType.UNKNOWN;
    }

    public static void callAttackEntityBodyFrom(Entity parent, DamageSource source, float amount, int partId) {
        if (!srpPresent || parent == null) return;
        try {
            attackEntityBodyFromMethod.invoke(parent, source, amount, partId, true);
        } catch (Exception ignored) {
        }
    }
}
