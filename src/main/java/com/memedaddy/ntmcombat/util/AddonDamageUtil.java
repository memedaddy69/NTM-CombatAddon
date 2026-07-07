package com.memedaddy.ntmcombat.util;

import com.hbm.lib.ModDamageSource;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IEntityLivingBaseAccessor;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IResistanceStatsAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
        if (type.equals("laser") || type.equals("plasma") || type.equals("microwave") ||
                type.equals("subatomic") || type.equals("electric")) {
            return "EN";
        }

        if (source == ModDamageSource.electricity || source == ModDamageSource.microwave) {
            return "EN";
        }

        return null;
    }

    /**
     * Compatibility helper: treat the given vanilla DamageSource (for example DamageSource.ON_FIRE)
     * as the damage source while still routing the hit through SEDNA reductions.
     * This allows other mods that check for exact vanilla DamageSource instances to react
     * while SEDNA DT/DR are still applied via the LivingHurtEvent.
     */
    public static boolean attackEntityFromNTUsingVanillaSource(EntityLivingBase living, DamageSource vanillaSource, Entity trueAttacker, float amount, boolean ignoreIFrame, boolean allowSpecialCancel, double knockbackMultiplier, float pierceDT, float pierce, boolean bypassVanillaArmor) {

        if (living instanceof EntityPlayerMP && trueAttacker instanceof EntityPlayer) {
            if (!((EntityPlayerMP) living).canAttackPlayer((EntityPlayer) trueAttacker))
                return false;
        }

        if (ignoreIFrame) {
            ((IEntityLivingBaseAccessor) living).setLastDamage(0F);
            living.hurtResistantTime = 0;
        }

        DamageResistanceHandler.setup(pierceDT, pierce);

        AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypassVanillaArmor);
        float reducedAmount = EntityDamageUtil.applyArmorCalculationsNT(living, vanillaSource, amount);

        AddonDamageUtil.isNTDamage.set(true);

        try {
            boolean ret = living.attackEntityFrom(vanillaSource, reducedAmount);

            Entity entity = trueAttacker != null ? trueAttacker : vanillaSource.getTrueSource();
            IEntityLivingBaseAccessor accessor = (IEntityLivingBaseAccessor) living;
            if (entity != null) {
                if (entity instanceof EntityLivingBase) {
                    living.setRevengeTarget((EntityLivingBase) entity);
                }

                if (entity instanceof EntityPlayer) {
                    accessor.setRecentlyHit(100);
                    accessor.setAttackingPlayer((EntityPlayer) entity);

                } else if (entity instanceof EntityTameable) {
                    if (((EntityTameable) entity).isTamed()) {
                        accessor.setRecentlyHit(100);
                        accessor.setAttackingPlayer(null);
                    }
                }

                if (knockbackMultiplier > 0) {
                    double deltaX = entity.posX - living.posX;
                    double deltaZ = entity.posZ - living.posZ;

                    for (double tmp = deltaZ; deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D; deltaZ = (Math.random() - Math.random()) * 0.01D) {
                        deltaX = (Math.random() - Math.random()) * 0.01D;
                    }

                    living.attackedAtYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - living.rotationYaw;

                    EntityDamageUtil.knockBack(living, entity, amount, deltaX, deltaZ, knockbackMultiplier);
                }
            }

            return ret;
        } finally {
            DamageResistanceHandler.reset();
            AddonDamageUtil.isNTDamage.remove();
            AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
        }
    }
}
