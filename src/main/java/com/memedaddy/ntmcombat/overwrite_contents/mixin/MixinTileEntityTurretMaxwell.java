package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.tileentity.turret.TileEntityTurretMaxwell;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityTurretMaxwell.class, remap = false)
public abstract class MixinTileEntityTurretMaxwell {

    @Redirect(
            method = "updateFiringTick",
            at = @At(value = "INVOKE", target = "Lcom/hbm/util/EntityDamageUtil;attackEntityFromIgnoreIFrame(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;F)Z", remap = false)
    )
    private boolean redirectMaxwellDamage(Entity victim, DamageSource src, float damage) {
        Entity unwrapped = AddonDamageUtil.unwrapMultiPart(victim);
        if (unwrapped instanceof EntityLivingBase living) {
            return EntityDamageUtil.attackEntityFromNT(living, src, damage, true, true, 0, 0, 0);
        }
        return EntityDamageUtil.attackEntityFromIgnoreIFrame(unwrapped, src, damage);
    }
}
