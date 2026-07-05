package com.yourname.ntmaddon.mixin;

import com.hbm.handler.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import com.yourname.ntmaddon.util.AddonDamageUtil; // Your new util class!
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

    // Assuming the method is static and returns a boolean.
    // If it returns void, use CallbackInfo instead.
    @Inject(method = "attackEntityFromNT", at = @At("HEAD"), remap = false)
    private static void onAttackStart(CallbackInfoReturnable<Boolean> cir) {
        AddonDamageState.isNTDamage.set(true);
    }

    // @At("TAIL") ensures the flag is lowered right before the method exits,
    // regardless of which 'return' statement was hit inside the method.
    @Inject(method = "attackEntityFromNT", at = @At("TAIL"), remap = false)
    private static void onAttackEnd(CallbackInfoReturnable<Boolean> cir) {
        AddonDamageState.isNTDamage.set(false);
    }
}
    /**
     * Overrides HBM's default armor calculation to use your custom toughness math.
     */
    @Inject(method = "applyArmorCalculationsNT", at = @At("HEAD"), cancellable = true)
    private static void injectCustomArmorCalculations(EntityLivingBase living, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        boolean bypassArmor = AddonDamageState.bypassVanillaArmorThisDamage.get();

        if (!source.isUnblockable() && !bypassArmor) {
            float armor = living.getTotalArmorValue();
            float toughness = (float) living.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();

            float pdr = DamageResistanceHandler.currentPDR.get();
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
                lastDamage = ((EntityLivingBase)victim).lastDamage;
            }
            float dmg = damage + lastDamage;
            cir.setReturnValue(victim.attackEntityFrom(src, dmg));
        } else {
            cir.setReturnValue(true);
        }
    }
}