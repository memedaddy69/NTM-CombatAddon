package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.memedaddy.ntmcombat.api.IBulletConfigAccessor;

@Mixin(value = XFactoryAccelerator.class, remap = false)
public class MixinXFactoryAccelerator {

	@Shadow
	public static BulletConfig tau_uranium;

	@Shadow
	public static BulletConfig tau_uranium_charge;

	@Inject(method = "init", at = @At("TAIL"))
	private static void postInit(CallbackInfo ci) {
		((IBulletConfigAccessor) (Object) tau_uranium).setBypassVanillaArmor(true);
		((IBulletConfigAccessor) (Object) tau_uranium_charge).setBypassVanillaArmor(true);
	}
}
