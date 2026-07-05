package com.memedaddy.ntmcombat.util;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IEntityLivingBaseAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;

public class AddonDamageUtil {

    public static Entity unwrapMultiPart(Entity entity) {
        if (entity instanceof MultiPartEntityPart part && part.parent instanceof Entity parent) {
            return parent;
        }
        return entity;
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
            // FIXED: Using the Accessor to bypass protected visibility
            ((IEntityLivingBaseAccessor) living).setLastDamage(0F);
            living.hurtResistantTime = 0;
        }

        // 1. Call base HBM's setup
        DamageResistanceHandler.setup(pierceDT, pierce);

        // 2. Set custom addon states to communicate with the Mixins
        AddonDamageState.isNTDamage.set(true);
        AddonDamageState.bypassVanillaArmorThisDamage.set(bypassVanillaArmor);

        try {
            // Call vanilla attackEntityFrom with the vanilla source while SEDNA is active so
            // DamageResistanceHandler.onEntityHurt applies DT/DR via LivingHurtEvent.
            boolean ret = living.attackEntityFrom(vanillaSource, amount);

            // Post-process: preserve attacker attribution and knockback using the provided trueAttacker
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

                    // FIXED: Routed to EntityDamageUtil instead of the missing local method
                    EntityDamageUtil.knockBack(living, entity, amount, deltaX, deltaZ, knockbackMultiplier);
                }
            }

            return ret;
        } finally {
            // 3. Reset base HBM
            DamageResistanceHandler.reset();

            // 4. Clean up addon states
            AddonDamageState.isNTDamage.remove();
            AddonDamageState.bypassVanillaArmorThisDamage.remove();
        }
    }
}
