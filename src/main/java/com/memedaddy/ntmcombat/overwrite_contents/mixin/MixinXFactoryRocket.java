package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.lib.ForgeDirection;
import com.memedaddy.ntmcombat.util.AddonFactoryHooks;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

@Mixin(value = XFactoryRocket.class, remap = false)
public class MixinXFactoryRocket {

	@Shadow
	public static BiConsumer<EntityBulletBaseMK4, RayTraceResult> LAMBDA_STANDARD_EXPLODE_HEAT;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void replaceLambdas(CallbackInfo ci) {
		LAMBDA_STANDARD_EXPLODE_HEAT = AddonFactoryHooks.LAMBDA_ROCKET_HEAT;
	}

	/**
	 * @reason Route incendiary explosions through FIRE-class EntityProcessorCrossSmooth for SRP compatibility
	 * @author NTM-CombatAddon
	 */
	@Overwrite
	public static void spawnFire(EntityBulletBaseMK4 bullet, RayTraceResult mop, boolean phosphorus, int duration) {
		if (mop.typeOfHit == RayTraceResult.Type.ENTITY && bullet.ticksExisted < 3) return;
		World world = bullet.world;
		ExplosionVNT vnt = new ExplosionVNT(world, mop.hitVec.x, mop.hitVec.y, mop.hitVec.z, 3F, bullet.getThrower());
		vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setDamageClass(com.hbm.util.DamageResistanceHandler.DamageClass.FIRE).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
		vnt.setPlayerProcessor(new PlayerProcessorStandard());
		vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
		vnt.explode();
		EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(duration).setType(phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL);
		fire.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
		world.spawnEntity(fire);
		bullet.setDead();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					int x = (int) Math.floor(mop.hitVec.x) + dx;
					int y = (int) Math.floor(mop.hitVec.y) + dy;
					int z = (int) Math.floor(mop.hitVec.z) + dz;
					BlockPos pos = new BlockPos(x, y, z);
					if (world.getBlockState(pos).getBlock().isAir(world.getBlockState(pos), bullet.world, pos)) for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
						if (world.getBlockState(pos.add(dir.offsetX, dir.offsetY, dir.offsetZ)).getBlock().isFlammable(world, pos.add(dir.offsetX, dir.offsetY, dir.offsetZ), Objects.requireNonNull(dir.getOpposite().toEnumFacing()))) {
							world.setBlockState(pos, Blocks.FIRE.getDefaultState());
							break;
						}
					}
				}
			}
		}
	}
}
