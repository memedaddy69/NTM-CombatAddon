package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.util.EntityDamageUtil;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import static com.hbm.util.DamageResistanceHandler.DamageClass;

@Mixin(value = EntityProcessorCrossSmooth.class, remap = false)
public class MixinEntityProcessorCrossSmooth {

	@Shadow
	protected DamageClass clazz;

	@Shadow
	protected float pierceDT;

	@Shadow
	protected float pierceDR;

	@Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
	public void injectAttackEntity(Entity entity, ExplosionVNT source, float amount, CallbackInfo ci) {
		if (!entity.isEntityAlive()) return;
		if (source.exploder == entity) amount *= 0.5F;

		Entity unwrapped = AddonDamageUtil.unwrapMultiPart(entity);

		if (!(unwrapped instanceof EntityLivingBase living)) {
			DamageSource dmg = BulletConfig.getDamage(null, source.exploder instanceof EntityLivingBase ? (EntityLivingBase) source.exploder : null, clazz);
			unwrapped.attackEntityFrom(dmg, amount);
			ci.cancel();
			return;
		}

		DamageSource dmg = BulletConfig.getDamage(null, source.exploder instanceof EntityLivingBase ? (EntityLivingBase) source.exploder : null, clazz);

		EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, pierceDT, pierceDR);

		if (!living.isEntityAlive()) ConfettiUtil.decideConfetti(living, dmg);

		ci.cancel();
	}
}
