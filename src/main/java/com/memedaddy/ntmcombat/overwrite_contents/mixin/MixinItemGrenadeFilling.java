package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling;
import com.memedaddy.ntmcombat.util.AddonFactoryHooks;

@Mixin(value = ItemGrenadeFilling.class, remap = false)
public class MixinItemGrenadeFilling {

	@Shadow
	@Final
	@Mutable
	public static Consumer<EntityGrenadeUniversal> EXPLODE_INC;

	@Shadow
	@Final
	@Mutable
	public static Consumer<EntityGrenadeUniversal> EXPLODE_WP;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void replaceLambdas(CallbackInfo ci) {
		EXPLODE_INC = AddonFactoryHooks.LAMBDA_GRENADE_INC;
		EXPLODE_WP = AddonFactoryHooks.LAMBDA_GRENADE_WP;
	}
}
