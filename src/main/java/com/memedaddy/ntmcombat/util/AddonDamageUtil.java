package com.memedaddy.ntmcombat.util;

import com.hbm.lib.ModDamageSource;
import com.hbm.util.DamageResistanceHandler;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IResistanceStatsAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.util.DamageSource;

import java.util.HashMap;
import java.util.Locale;

public class AddonDamageUtil {

    // Defaults to false. ThreadLocal ensures absolute safety during calculation chains.
    public static final ThreadLocal<Boolean> isNTDamage = ThreadLocal.withInitial(() -> false);
    // Custom flag to bypass vanilla armor math
    public static final ThreadLocal<Boolean> bypassVanillaArmorThisDamage = ThreadLocal.withInitial(() -> false);

    public static Entity unwrapMultiPart(Entity entity) {
        if (entity instanceof MultiPartEntityPart) {
            MultiPartEntityPart part = (MultiPartEntityPart) entity;
            if (part.parent instanceof Entity) {
                return (Entity) part.parent;
            }
        }
        return entity;
    }

    public static float getElementalDR(DamageResistanceHandler.ResistanceStats stats, DamageSource damage) {
        IResistanceStatsAccessor accessor = (IResistanceStatsAccessor) stats;

        HashMap<String, DamageResistanceHandler.Resistance> exactMap = accessor.getExactResistances();
        DamageResistanceHandler.Resistance exact = exactMap.get(damage.getDamageType());

        if (exact != null) {
            String category = getElementalCategory(damage);
            if (category != null) {
                return exact.resistance;
            }
        }

        String category = getElementalCategory(damage);
        if (category != null) {
            HashMap<String, DamageResistanceHandler.Resistance> categoryMap = accessor.getCategoryResistances();
            DamageResistanceHandler.Resistance catRes = categoryMap.get(category);
            if (catRes != null) {
                return catRes.resistance;
            }
        }

        return 0F;
    }

    private static String getElementalCategory(DamageSource source) {
        if (source.isExplosion()) return "EXPL";
        if (source.isFireDamage()) return "FIRE";

        String type = source.getDamageType().toLowerCase(Locale.US);

        if (type.equals("fire")) return "FIRE";
        if (type.equals("artillery") || type.equals("himars")) return "EXPL";
        if (type.equals("laser") || type.equals("plasma") || type.equals("microwave") ||
                type.equals("subatomic") || type.equals("electric")) {
            return "EN";
        }

        if (source == ModDamageSource.electricity || source == ModDamageSource.microwave) {
            return "EN";
        }

        return null;
    }

}
