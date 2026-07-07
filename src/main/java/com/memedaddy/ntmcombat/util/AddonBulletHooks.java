package com.memedaddy.ntmcombat.util;

import java.util.function.BiConsumer;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.api.IBulletConfigAccessor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;

public class AddonBulletHooks {

    public static final BiConsumer<EntityBulletBaseMK4, RayTraceResult> LAMBDA_STANDARD_ENTITY_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity entity = AddonDamageUtil.unwrapMultiPart(mop.entityHit);

            if (entity == bullet.getThrower() && bullet.ticksExisted < bullet.selfDamageDelay()) return;
            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = bullet.config.getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
            float intendedDamage = bullet.damage;

            if (!(entity instanceof EntityLivingBase)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                return;
            } else if (bullet.config.headshotMult > 1F) {

                EntityLivingBase living = (EntityLivingBase) entity;
                double head = living.height - living.getEyeHeight();

                if (!!living.isEntityAlive() && mop.hitVec != null && mop.hitVec.y > (living.posY + living.height - head * 2)) {
                    intendedDamage *= bullet.config.headshotMult;
                }
            }

            EntityLivingBase living = (EntityLivingBase) entity;
            float prevHealth = living.getHealth();

            boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();

            if (bullet.config.dmgClass == DamageResistanceHandler.DamageClass.FIRE) {
                EntityLivingBase trueAttacker = bullet.getThrower();
                AddonDamageUtil.attackEntityFromNTUsingVanillaSource(living, DamageSource.ON_FIRE, trueAttacker, intendedDamage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent, bypass);
            } else {
                AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
                try {
                    EntityDamageUtil.attackEntityFromNT(living, source, intendedDamage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);
                } finally {
                    AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
                }
            }

            float newHealth = living.getHealth();

            if (bullet.config.damageFalloffByPen) bullet.damage -= (float) (Math.max(prevHealth - newHealth, 0) * 0.5);
            if (!bullet.doesPenetrate() || bullet.damage < 0) {
                bullet.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
                bullet.setDead();
            }

            if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    public static final BiConsumer<EntityBulletBeamBase, RayTraceResult> LAMBDA_STANDARD_BEAM_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity entity = AddonDamageUtil.unwrapMultiPart(mop.entityHit);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = bullet.config.getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);

            if (!(entity instanceof EntityLivingBase)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                return;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();

            if (bullet.config.dmgClass == DamageResistanceHandler.DamageClass.FIRE) {
                EntityLivingBase trueAttacker = bullet.getThrower();
                AddonDamageUtil.attackEntityFromNTUsingVanillaSource(living, DamageSource.ON_FIRE, trueAttacker, bullet.damage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent, bypass);
            } else {
                AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
                try {
                    EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);
                } finally {
                    AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
                }
            }

            if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    public static final BiConsumer<EntityBulletBeamBase, RayTraceResult> LAMBDA_BEAM_HIT = (beam, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity entity = AddonDamageUtil.unwrapMultiPart(mop.entityHit);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = beam.config.getDamage(beam, beam.thrower, beam.config.dmgClass);

            if (!(entity instanceof EntityLivingBase)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
                return;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            boolean bypass = ((IBulletConfigAccessor) (Object) beam.config).isBypassVanillaArmor();

            if (beam.config.dmgClass == DamageResistanceHandler.DamageClass.FIRE) {
                EntityLivingBase trueAttacker = beam.thrower;
                AddonDamageUtil.attackEntityFromNTUsingVanillaSource(living, DamageSource.ON_FIRE, trueAttacker, beam.damage, true, false, beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent, bypass);
            } else {
                AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
                try {
                    EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false, beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent);
                } finally {
                    AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
                }
            }
        }
    };
}
