package com.memedaddy.ntmcombat.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MultiPartEntityPart;
import com.hbm.config.ServerConfig;
import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.Untested;
import com.hbm.items.ModItems;
import com.hbm.lib.internal.MethodHandleHelper;
import com.hbm.main.MainRegistry;
import net.minecraft.entity.EntityLivingBase;
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
        if (living instanceof EntityPlayerMP playerMP && trueAttacker instanceof EntityPlayer attacker) {
            if (!playerMP.canAttackPlayer(attacker))
                return false;
        }

        if (ignoreIFrame) { living.lastDamage = 0F; living.hurtResistantTime = 0; }

        DamageResistanceHandler.setup(pierceDT, pierce, bypassVanillaArmor);

        try {
            // Call vanilla attackEntityFrom with the vanilla source while SEDNA is active so
            // DamageResistanceHandler.onEntityHurt applies DT/DR via LivingHurtEvent.
            boolean ret = living.attackEntityFrom(vanillaSource, amount);

            // Post-process: preserve attacker attribution and knockback using the provided trueAttacker
            Entity entity = trueAttacker != null ? trueAttacker : vanillaSource.getTrueSource();
            if (entity != null) {
                if (entity instanceof EntityLivingBase entityLivingBase) {
                    living.setRevengeTarget(entityLivingBase);
                }

                if (entity instanceof EntityPlayer player) {
                    living.recentlyHit = 100;
                    living.attackingPlayer = player;

                } else if (entity instanceof EntityTameable entitywolf) {

                    if (entitywolf.isTamed()) {
                        living.recentlyHit = 100;
                        living.attackingPlayer = null;
                    }
                }

                if (knockbackMultiplier > 0) {
                    double deltaX = entity.posX - living.posX;
                    double deltaZ = entity.posZ - living.posZ;

                    for (double tmp = deltaZ; deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D; deltaZ = (Math.random() - Math.random()) * 0.01D) {
                        deltaX = (Math.random() - Math.random()) * 0.01D;
                    }

                    living.attackedAtYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - living.rotationYaw;
                    knockBack(living, entity, (float) amount, deltaX, deltaZ, knockbackMultiplier);
                }
            }

            return ret;
        } finally {
            DamageResistanceHandler.reset();
        }
    }

}
