package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.memedaddy.ntmcombat.api.IBulletConfigAccessor;

@Mixin(value = XFactoryFlamer.class, remap = false)
public class MixinXFactoryFlamer {

	@Shadow
	public static BulletConfig flame_diesel;

	@Shadow
	public static BulletConfig flame_gas;

	@Shadow
	public static BulletConfig flame_napalm;

	@Shadow
	public static BulletConfig flame_balefire;

	@Inject(method = "init", at = @At("TAIL"))
	private static void postInit(CallbackInfo ci) {
		((IBulletConfigAccessor) (Object) flame_diesel).setBypassVanillaArmor(true);
		((IBulletConfigAccessor) (Object) flame_gas).setBypassVanillaArmor(true);
		((IBulletConfigAccessor) (Object) flame_napalm).setBypassVanillaArmor(true);
		((IBulletConfigAccessor) (Object) flame_balefire).setBypassVanillaArmor(true);
	}
}
