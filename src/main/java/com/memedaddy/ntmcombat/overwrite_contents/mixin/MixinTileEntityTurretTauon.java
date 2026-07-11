package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.tileentity.turret.TileEntityTurretTauon;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityTurretTauon.class, remap = false)
public abstract class MixinTileEntityTurretTauon {

    @Redirect(
            method = "updateFiringTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z")
    )
    private boolean redirectTauonDamage(Entity target, DamageSource source, float amount) {
        Entity unwrapped = AddonDamageUtil.unwrapMultiPart(target);
        source.setDamageIsAbsolute();
        if (unwrapped instanceof EntityLivingBase living) {
            return EntityDamageUtil.attackEntityFromNT(living, source, amount, true, true, 0, 0, 0);
        }
        return EntityDamageUtil.attackEntityFromIgnoreIFrame(unwrapped, source, amount);
    }
}
