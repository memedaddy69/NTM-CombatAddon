package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.memedaddy.ntmcombat.api.IBulletConfigAccessor;
import com.memedaddy.ntmcombat.util.AddonBulletHooks;

@Mixin(value = BulletConfig.class, remap = false)
public abstract class MixinBulletConfig implements IBulletConfigAccessor {

	public boolean bypassVanillaArmor = false;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void replaceLambdas(CallbackInfo ci) {
		BulletConfig.LAMBDA_STANDARD_ENTITY_HIT = AddonBulletHooks.LAMBDA_STANDARD_ENTITY_HIT;
		BulletConfig.LAMBDA_STANDARD_BEAM_HIT = AddonBulletHooks.LAMBDA_STANDARD_BEAM_HIT;
		BulletConfig.LAMBDA_BEAM_HIT = AddonBulletHooks.LAMBDA_BEAM_HIT;
	}

	@Override
	public boolean isBypassVanillaArmor() {
		return this.bypassVanillaArmor;
	}

	@Override
	public BulletConfig setBypassVanillaArmor(boolean bypass) {
		this.bypassVanillaArmor = bypass;
		return (BulletConfig) (Object) this;
	}
}
