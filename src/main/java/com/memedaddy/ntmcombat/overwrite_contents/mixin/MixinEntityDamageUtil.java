package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.handler.SRPMobAdaptReflect;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityDamageUtil.class, remap = false)
public class MixinEntityDamageUtil {

    // ADDED: The original method parameters (entity, source, amount) before the cir
    @Inject(method = "attackEntityFromNT", at = @At("HEAD"), remap = false)
    private static void onAttackStart(EntityLivingBase living, DamageSource source, float amount, boolean ignoreIFrame, boolean allowSpecialCancel, double knockbackMultiplier, float pierceDT, float pierce, CallbackInfoReturnable<Boolean> cir) {
        AddonDamageUtil.isNTDamage.set(true);
    }

    // ADDED: The original method parameters (entity, source, amount) before the cir
    @Inject(method = "attackEntityFromNT", at = @At("TAIL"), remap = false)
    private static void onAttackEnd(EntityLivingBase living, DamageSource source, float amount, boolean ignoreIFrame, boolean allowSpecialCancel, double knockbackMultiplier, float pierceDT, float pierce, CallbackInfoReturnable<Boolean> cir) {
        AddonDamageUtil.isNTDamage.set(false);
    }

    // REMOVED: The stray '}' that was right here closing the class prematurely

    /**
     * Overrides HBM's default armor calculation to use your custom toughness math.
     */
    @Inject(method = "applyArmorCalculationsNT", at = @At("HEAD"), cancellable = true)
    private static void injectCustomArmorCalculations(EntityLivingBase living, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (source.isUnblockable() || AddonDamageUtil.bypassVanillaArmorThisDamage.get()) {
            cir.setReturnValue(amount);
            return;
        }

        float armor = living.getTotalArmorValue();
        float toughness = (float) living.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();

        float pdr = DamageResistanceHandler.currentPDR;
        float effectiveArmor = armor * (1 - pdr);

        float newAmount = CombatRules.getDamageAfterAbsorb(amount, effectiveArmor, toughness);

        cir.setReturnValue(newAmount);
    }

    /**
     * Overrides the iFrame ignore method to utilize your multipart unwrapper.
     */
    @Inject(method = "attackEntityFromIgnoreIFrame", at = @At("HEAD"), cancellable = true)
    private static void injectIFrameIgnore(Entity victim, DamageSource src, float damage, CallbackInfoReturnable<Boolean> cir) {

        // Call the unwrapper from your own utility class
        victim = AddonDamageUtil.unwrapMultiPart(victim);

        if (!victim.attackEntityFrom(src, damage)) {
            float lastDamage = 0;
            if (victim instanceof EntityLivingBase) {
                lastDamage = ((IEntityLivingBaseAccessor) victim).getLastDamage();
            }
            float dmg = damage + lastDamage;
            cir.setReturnValue(victim.attackEntityFrom(src, dmg));
        } else {
            cir.setReturnValue(true);
        }
    }

    /**
     * SRP Parasite Entity Adaptation: intercepts damage flowing through the SEDNA
     * pipeline and applies SRP's entity-level adaptation reduction before armor/potion
     * calculations run (only when SRP is loaded).
     */
    @ModifyVariable(method = "damageEntityNT", at = @At("HEAD"), argsOnly = true)
    private static float injectSRPAdaptation(float amount, EntityLivingBase living, DamageSource source) {
        if (SRPMobAdaptReflect.skipNextDamageEntityCall) {
            System.out.println("[SRP-TRACE] NESTED BLOCKED by skipNextDamageEntityCall flag"
                    + " living=" + living.getClass().getSimpleName() + "@" + Integer.toHexString(living.hashCode())
                    + " tick=" + living.ticksExisted + " amount=" + amount);
            return amount;
        }
        return SRPMobAdaptReflect.applyAdaptation(living, source, amount);
    }
} // <- The class now correctly closes down here.