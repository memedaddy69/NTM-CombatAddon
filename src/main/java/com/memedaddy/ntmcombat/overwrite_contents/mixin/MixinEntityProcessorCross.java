package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

@Mixin(value = EntityProcessorCross.class, remap = false)
public class MixinEntityProcessorCross {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void injectAttackEntity(Entity entity, ExplosionVNT source, float amount, CallbackInfo ci) {
        DamageSource dmgSource = EntityProcessorCross.setExplosionSource(source.compat);
        Entity unwrapped = AddonDamageUtil.unwrapMultiPart(entity);
        if (unwrapped instanceof EntityLivingBase living) {
            EntityDamageUtil.attackEntityFromNT(living, dmgSource, amount, true, true, 0, 0, 0);
        } else {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(unwrapped, dmgSource, amount);
        }
        ci.cancel();
    }
}
