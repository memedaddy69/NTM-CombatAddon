package com.memedaddy.ntmcombat.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.api.IBulletConfigAccessor;
import com.memedaddy.ntmcombat.handler.SRPMobAdaptReflect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;

public class AddonBulletHooks {

    private static boolean isPlasmaDamage(DamageSource source) {
        return source.getDamageType().toLowerCase(java.util.Locale.US).equals("plasma");
    }

    private static void applyNTHelper(EntityLivingBase living, DamageSource source, float damage,
                                       float knockbackMult, float armorDT, float armorPierce, boolean bypass) {
        AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
        try {
            EntityDamageUtil.attackEntityFromNT(living, source, damage, true, true,
                    knockbackMult, armorDT, armorPierce);
        } finally {
            AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
        }
    }

    // ------------------------------------------------------------
    //  GENERAL-PURPOSE PIERCING DEDUP
    // ------------------------------------------------------------

    private static final ThreadLocal<Map<Integer, Set<Integer>>> DEDUP_TRACKER = ThreadLocal.withInitial(HashMap::new);

    private static boolean pierceDedupCheck(int projectileKey, int rootEntityId) {
        Map<Integer, Set<Integer>> map = DEDUP_TRACKER.get();
        Set<Integer> set = map.get(projectileKey);
        if (set == null) {
            set = new HashSet<>();
            map.put(projectileKey, set);
        }
        boolean result = set.contains(rootEntityId);
        if (!result) set.add(rootEntityId);
        return result;
    }

    private static void pierceDedupCleanup(int projectileKey) {
        DEDUP_TRACKER.get().remove(projectileKey);
    }

    private static int beamKey(EntityBulletBeamBase beam) {
        return beam.getEntityId() ^ (beam.ticksExisted << 16);
    }

    // ------------------------------------------------------------
    //  LAMBDA_STANDARD_ENTITY_HIT  (EntityBulletBaseMK4)
    // ------------------------------------------------------------
    public static final BiConsumer<EntityBulletBaseMK4, RayTraceResult> LAMBDA_STANDARD_ENTITY_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = mop.entityHit;
            boolean isSRPBody = SRPMultipartUtil.isEntityBody(hitEntity);
            Entity entity = isSRPBody ? hitEntity : AddonDamageUtil.unwrapMultiPart(hitEntity);

            if (entity == bullet.getThrower() && bullet.ticksExisted < bullet.selfDamageDelay()) return;
            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = bullet.config.getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
            float intendedDamage = bullet.damage;
            boolean isPiercing = bullet.config.doesPenetrate;

            int projKey = bullet.getEntityId();

            // ---- EARLY DEDUP: avoid re-damaging the same entity ----
            Entity rootEntity = null;
            if (isSRPBody) {
                rootEntity = SRPMultipartUtil.getParent(hitEntity);
            } else {
                Entity unwrapped = AddonDamageUtil.unwrapMultiPart(hitEntity);
                if (unwrapped != hitEntity) rootEntity = unwrapped;
            }
            if (rootEntity == null) rootEntity = entity;
            if (pierceDedupCheck(projKey, rootEntity.getEntityId())) {
                if (isSRPBody) {
                    int partId = SRPMultipartUtil.getPartId(hitEntity);
                    SRPMultipartUtil.callAttackEntityBodyFrom(rootEntity, source, intendedDamage, partId);
                    if (rootEntity instanceof EntityLivingBase) SRPMobAdaptReflect.refreshHIT((EntityLivingBase) rootEntity, source);
                }
                return;
            }

            // ---- SRP BODY PART HANDLING ----
            if (isSRPBody) {
                int partId = SRPMultipartUtil.getPartId(hitEntity);
                SRPMultipartUtil.BodyPartType partType = SRPMultipartUtil.getBodyPartType(partId);
                Entity parent = SRPMultipartUtil.getParent(hitEntity);
                boolean isPlasma = isPlasmaDamage(source);

                if (isPiercing && parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();
                    applyNTHelper(living, source, intendedDamage,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent,
                            bypass);
                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, intendedDamage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }

                // ---- NON-PIERCING: existing behavior ----
                if (partType == SRPMultipartUtil.BodyPartType.TENDRIL && !isPlasma) {
                } else if (parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();
                    float prevHealth = living.getHealth();

                    applyNTHelper(living, source, intendedDamage,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent,
                            bypass);

                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, intendedDamage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    float newHealth = living.getHealth();

                    if (bullet.config.damageFalloffByPen)
                        bullet.damage -= (float) (Math.max(prevHealth - newHealth, 0) * 0.5);

                    if (!bullet.doesPenetrate() || bullet.damage < 0) {
                        pierceDedupCleanup(projKey);
                        bullet.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
                        bullet.setDead();
                    }

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }
            }

            // ---- EXISTING CODE (unchanged) for non-SRP / non-plasma tendrils ----
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

            AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
            try {
                EntityDamageUtil.attackEntityFromNT(living, source, intendedDamage, true, true,
                        bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);
            } finally {
                AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
            }

            float newHealth = living.getHealth();

            if (bullet.config.damageFalloffByPen)
                bullet.damage -= (float) (Math.max(prevHealth - newHealth, 0) * 0.5);

            if (!bullet.doesPenetrate() || bullet.damage < 0) {
                pierceDedupCleanup(projKey);
                bullet.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
                bullet.setDead();
            }

            if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    // ------------------------------------------------------------
    //  LAMBDA_STANDARD_BEAM_HIT  (EntityBulletBeamBase)
    // ------------------------------------------------------------
    public static final BiConsumer<EntityBulletBeamBase, RayTraceResult> LAMBDA_STANDARD_BEAM_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = mop.entityHit;
            boolean isSRPBody = SRPMultipartUtil.isEntityBody(hitEntity);
            Entity entity = isSRPBody ? hitEntity : AddonDamageUtil.unwrapMultiPart(hitEntity);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = bullet.config.getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
            boolean isPiercing = bullet.config.doesPenetrate;

            int projKey = beamKey(bullet);

            // ---- EARLY DEDUP: avoid re-damaging the same entity ----
            Entity rootEntity = null;
            if (isSRPBody) {
                rootEntity = SRPMultipartUtil.getParent(hitEntity);
            } else {
                Entity unwrapped = AddonDamageUtil.unwrapMultiPart(hitEntity);
                if (unwrapped != hitEntity) rootEntity = unwrapped;
            }
            if (rootEntity == null) rootEntity = entity;
            if (pierceDedupCheck(projKey, rootEntity.getEntityId())) {
                if (isSRPBody) {
                    int partId = SRPMultipartUtil.getPartId(hitEntity);
                    SRPMultipartUtil.callAttackEntityBodyFrom(rootEntity, source, bullet.damage, partId);
                    if (rootEntity instanceof EntityLivingBase) SRPMobAdaptReflect.refreshHIT((EntityLivingBase) rootEntity, source);
                }
                return;
            }

            // ---- SRP BODY PART HANDLING ----
            if (isSRPBody) {
                int partId = SRPMultipartUtil.getPartId(hitEntity);
                SRPMultipartUtil.BodyPartType partType = SRPMultipartUtil.getBodyPartType(partId);
                Entity parent = SRPMultipartUtil.getParent(hitEntity);
                boolean isPlasma = isPlasmaDamage(source);

                if (isPiercing && parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();
                    applyNTHelper(living, source, bullet.damage,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent,
                            bypass);
                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, bullet.damage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }

                // ---- NON-PIERCING: existing behavior ----
                if (partType == SRPMultipartUtil.BodyPartType.TENDRIL && !isPlasma) {
                } else if (parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();
                    applyNTHelper(living, source, bullet.damage,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent,
                            bypass);
                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, bullet.damage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }
            }

            // ---- EXISTING CODE ----
            if (!(entity instanceof EntityLivingBase)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                return;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            boolean bypass = ((IBulletConfigAccessor) (Object) bullet.config).isBypassVanillaArmor();

            AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
            try {
                EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage, true, true,
                        bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);
            } finally {
                AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
            }

            if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    // ------------------------------------------------------------
    //  LAMBDA_BEAM_HIT  (EntityBulletBeamBase, alternate field access)
    // ------------------------------------------------------------
    public static final BiConsumer<EntityBulletBeamBase, RayTraceResult> LAMBDA_BEAM_HIT = (beam, mop) -> {

        if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = mop.entityHit;
            boolean isSRPBody = SRPMultipartUtil.isEntityBody(hitEntity);
            Entity entity = isSRPBody ? hitEntity : AddonDamageUtil.unwrapMultiPart(hitEntity);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0) return;

            DamageSource source = beam.config.getDamage(beam, beam.thrower, beam.config.dmgClass);
            boolean isPiercing = beam.config.doesPenetrate;

            int projKey = beamKey(beam);

            // ---- EARLY DEDUP: avoid re-damaging the same entity ----
            Entity rootEntity = null;
            if (isSRPBody) {
                rootEntity = SRPMultipartUtil.getParent(hitEntity);
            } else {
                Entity unwrapped = AddonDamageUtil.unwrapMultiPart(hitEntity);
                if (unwrapped != hitEntity) rootEntity = unwrapped;
            }
            if (rootEntity == null) rootEntity = entity;
            if (pierceDedupCheck(projKey, rootEntity.getEntityId())) {
                if (isSRPBody) {
                    int partId = SRPMultipartUtil.getPartId(hitEntity);
                    SRPMultipartUtil.callAttackEntityBodyFrom(rootEntity, source, beam.damage, partId);
                    if (rootEntity instanceof EntityLivingBase) SRPMobAdaptReflect.refreshHIT((EntityLivingBase) rootEntity, source);
                }
                return;
            }

            // ---- SRP BODY PART HANDLING ----
            if (isSRPBody) {
                int partId = SRPMultipartUtil.getPartId(hitEntity);
                SRPMultipartUtil.BodyPartType partType = SRPMultipartUtil.getBodyPartType(partId);
                Entity parent = SRPMultipartUtil.getParent(hitEntity);
                boolean isPlasma = isPlasmaDamage(source);

                if (isPiercing && parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) beam.config).isBypassVanillaArmor();
                    AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
                    try {
                        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage,
                                true, false,
                                beam.config.knockbackMult,
                                beam.config.armorThresholdNegation,
                                beam.config.armorPiercingPercent);
                    } finally {
                        AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
                    }
                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, beam.damage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }

                // ---- NON-PIERCING: existing behavior ----
                if (partType == SRPMultipartUtil.BodyPartType.TENDRIL && !isPlasma) {
                } else if (parent instanceof EntityLivingBase living) {
                    boolean bypass = ((IBulletConfigAccessor) (Object) beam.config).isBypassVanillaArmor();
                    AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
                    try {
                        EntityDamageUtil.attackEntityFromNT(living, source, beam.damage,
                                true, false,
                                beam.config.knockbackMult,
                                beam.config.armorThresholdNegation,
                                beam.config.armorPiercingPercent);
                    } finally {
                        AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
                    }
                    SRPMobAdaptReflect.skipNextDamageEntityCall = true;
                    try {
                        SRPMultipartUtil.callAttackEntityBodyFrom(parent, source, beam.damage, partId);
                    } finally {
                        SRPMobAdaptReflect.skipNextDamageEntityCall = false;
                    }
                    SRPMobAdaptReflect.refreshHIT(living, source);

                    if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, source);
                    return;
                }
            }

            // ---- EXISTING CODE ----
            if (!(entity instanceof EntityLivingBase)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
                return;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            boolean bypass = ((IBulletConfigAccessor) (Object) beam.config).isBypassVanillaArmor();

            AddonDamageUtil.bypassVanillaArmorThisDamage.set(bypass);
            try {
                EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false,
                        beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent);
            } finally {
                AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
            }
        }
    };
}
