package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityProcessorCross.class, remap = false)
public class MixinEntityProcessorCross {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void redirectAttack(Entity entity, ExplosionVNT source, float amount, CallbackInfo ci) {
        if (!entity.isEntityAlive()) return;

        // artillery/HIMARS use the 5-param ExplosionVNT(world, x, y, z, size) constructor
        // which sets exploder = null.  Exploder may also be the actual shell/rocket entity.
        Entity exploder = source.exploder;
        boolean isArtillery = exploder instanceof EntityArtilleryShell;
        boolean isHIMARS = exploder instanceof EntityArtilleryRocket;

        if (!isArtillery && !isHIMARS && exploder != null) return;
        // exploder == null → artillery/HIMARS (base EntityProcessorCross used only by these)

        EntityLivingBase shooter = null;
        if (exploder instanceof EntityLivingBase exploderLiving) {
            shooter = exploderLiving;
        } else if (source.compat != null) {
            shooter = source.compat.getExplosivePlacedBy();
        }

        if (!(entity instanceof EntityLivingBase living)) {
            DamageSource dmg = isArtillery
                    ? AddonDamageSource.artillery(exploder, null)
                    : AddonDamageSource.himars(exploder, null);
            entity.attackEntityFrom(dmg, amount);
            ci.cancel();
            return;
        }

        DamageSource dmg = isHIMARS
                ? AddonDamageSource.himars(exploder, shooter)
                : AddonDamageSource.artillery(exploder, shooter);

        EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, 0F, 0F);
        ci.cancel();
    }
}
