package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageState;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityDamageUtil.class, remap = false)
public class MixinEntityDamageUtil {

    // ADDED: The original method parameters (entity, source, amount) before the cir
    @Inject(method = "attackEntityFromNT", at = @At("HEAD"), remap = false)
    private static void onAttackStart(Entity entity, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AddonDamageState.isNTDamage.set(true);
    }

    // ADDED: The original method parameters (entity, source, amount) before the cir
    @Inject(method = "attackEntityFromNT", at = @At("TAIL"), remap = false)
    private static void onAttackEnd(Entity entity, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AddonDamageState.isNTDamage.set(false);
    }

    // REMOVED: The stray '}' that was right here closing the class prematurely

    /**
     * Overrides HBM's default armor calculation to use your custom toughness math.
     */
    @Inject(method = "applyArmorCalculationsNT", at = @At("HEAD"), cancellable = true)
    private static void injectCustomArmorCalculations(EntityLivingBase living, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        boolean bypassArmor = AddonDamageState.bypassVanillaArmorThisDamage.get();

        if (!source.isUnblockable() && !bypassArmor) {
            float armor = living.getTotalArmorValue();
            float toughness = (float) living.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();

            // FIXED: Removed .get() assuming this is a native HBM float.
            // (If you meant to use your custom state, it should be AddonDamageState.currentPDR.get())
            float pdr = DamageResistanceHandler.currentPDR;
            float effectiveArmor = armor * (1 - pdr);

            float newAmount = CombatRules.getDamageAfterAbsorb(amount, effectiveArmor, toughness);

            // Return your new amount and cancel HBM's original math
            cir.setReturnValue(newAmount);
        }
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
} // <- The class now correctly closes down here.