package com.memedaddy.ntmcombat.util;

import com.hbm.lib.ModDamageSource;
import com.hbm.util.DamageResistanceHandler;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IResistanceStatsAccessor;
import net.minecraft.util.DamageSource;
import java.util.Locale;
import java.util.HashMap;

public class AddonDamageHelper {

    public static float getElementalDR(DamageResistanceHandler.ResistanceStats stats, DamageSource damage) {
        IResistanceStatsAccessor accessor = (IResistanceStatsAccessor) stats;

        // 1. Check Exact Resistances first
        HashMap<String, DamageResistanceHandler.Resistance> exactMap = accessor.getExactResistances();
        DamageResistanceHandler.Resistance exact = exactMap.get(damage.getDamageType());

        if (exact != null) {
            // We still need to verify the exact damage isn't physical before returning
            String category = getElementalCategory(damage);
            if (category != null) {
                return exact.resistance; // Return DR only, ignore DT (threshold)
            }
        }

        // 2. Check Category Resistances
        String category = getElementalCategory(damage);
        if (category != null) {
            HashMap<String, DamageResistanceHandler.Resistance> categoryMap = accessor.getCategoryResistances();
            DamageResistanceHandler.Resistance catRes = categoryMap.get(category);
            if (catRes != null) {
                return catRes.resistance; // Return DR only
            }
        }

        // Return 0 if it's Physical, Other, or unmapped
        return 0F;
    }

    private static String getElementalCategory(DamageSource source) {
        if (source.isExplosion()) return "EXPL"; //[cite: 3]
        if (source.isFireDamage()) return "FIRE"; //[cite: 3]

        String type = source.getDamageType().toLowerCase(Locale.US);
        if (type.equals("laser") || type.equals("plasma") || type.equals("microwave") ||
                type.equals("subatomic") || type.equals("electric")) { //[cite: 3]
            return "EN";
        }

        if (source == ModDamageSource.electricity || source == ModDamageSource.microwave) { //[cite: 3]
            return "EN";
        }

        // Implicitly ignores source.isProjectile(), CACTUS, spikes, EntityDamageSource, etc.[cite: 3]
        return null;
    }
}
