package com.memedaddy.ntmcombat.util;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class AddonFactoryHooks {

	public static final BiConsumer<EntityBulletBaseMK4, RayTraceResult> LAMBDA_40MM_HEAT = (bullet, mop) -> {
		if (mop.typeOfHit == mop.typeOfHit.ENTITY && bullet.ticksExisted < 3 && mop.entityHit == bullet.getThrower()) return;
		Lego.standardExplode(bullet, mop, 3.5F); bullet.setDead();
		if (mop.typeOfHit == mop.typeOfHit.ENTITY) {
			Entity entityHit = AddonDamageUtil.unwrapMultiPart(mop.entityHit);
			if (entityHit instanceof EntityLivingBase living) {
				AddonDamageUtil.bypassVanillaArmorThisDamage.set(false);
				try {
					EntityDamageUtil.attackEntityFromNT(living, bullet.config.getDamage(bullet, bullet.getThrower(), DamageResistanceHandler.DamageClass.EXPLOSIVE), bullet.damage * 3F, true, true, 0.5F, 3F, 0.15F);
				} finally {
					AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
				}
			} else {
				entityHit.attackEntityFrom(bullet.config.getDamage(bullet, bullet.getThrower(), DamageResistanceHandler.DamageClass.EXPLOSIVE), bullet.damage * 3F);
			}
		}
	};

	public static final BiConsumer<EntityBulletBaseMK4, RayTraceResult> LAMBDA_ROCKET_HEAT = (bullet, mop) -> {
		if (mop.typeOfHit == RayTraceResult.Type.ENTITY && bullet.ticksExisted < 3) return;
		Lego.standardExplode(bullet, mop, 3.5F); bullet.setDead();
		if (mop.typeOfHit == RayTraceResult.Type.ENTITY) {
			Entity entityHit = AddonDamageUtil.unwrapMultiPart(mop.entityHit);
			if (entityHit instanceof EntityLivingBase living) {
				AddonDamageUtil.bypassVanillaArmorThisDamage.set(false);
				try {
					EntityDamageUtil.attackEntityFromNT(living, BulletConfig.getDamage(bullet, bullet.getThrower(), DamageResistanceHandler.DamageClass.EXPLOSIVE), bullet.damage * 3F, true, true, 0.5F, 5F, 0.2F);
				} finally {
					AddonDamageUtil.bypassVanillaArmorThisDamage.remove();
				}
			} else {
				entityHit.attackEntityFrom(BulletConfig.getDamage(bullet, bullet.getThrower(), DamageResistanceHandler.DamageClass.EXPLOSIVE), bullet.damage * 3F);
			}
		}
	};

	public static final Consumer<EntityGrenadeUniversal> LAMBDA_GRENADE_INC = grenade -> {
		World world = grenade.world;
		ExplosionVNT vnt = new ExplosionVNT(world, grenade.posX, grenade.posY, grenade.posZ, 3F, grenade.getThrower());
		vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F).setDamageClass(DamageResistanceHandler.DamageClass.FIRE));
		vnt.setPlayerProcessor(new PlayerProcessorStandard());
		vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
		vnt.explode();
		EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(200).setType(EntityFireLingering.TYPE_DIESEL);
		fire.setPosition(grenade.posX, grenade.posY, grenade.posZ);
		world.spawnEntity(fire);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockPos bp = new BlockPos((int) Math.floor(grenade.posX) + dx, (int) Math.floor(grenade.posY) + dy, (int) Math.floor(grenade.posZ) + dz);
					if (!world.isAirBlock(bp)) continue;
					for (EnumFacing dir : EnumFacing.values()) {
						BlockPos np = bp.offset(dir);
						if (world.getBlockState(np).getBlock().isFlammable(world, np, dir.getOpposite())) {
							world.setBlockState(bp, Blocks.FIRE.getDefaultState());
							break;
						}
					}
				}
			}
		}
	};

	public static final Consumer<EntityGrenadeUniversal> LAMBDA_GRENADE_WP = grenade -> {
		World world = grenade.world;
		ExplosionVNT vnt = new ExplosionVNT(world, grenade.posX, grenade.posY, grenade.posZ, 3F, grenade.getThrower());
		vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F).setDamageClass(DamageResistanceHandler.DamageClass.FIRE));
		vnt.setPlayerProcessor(new PlayerProcessorStandard());
		vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
		vnt.explode();
		EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(600).setType(EntityFireLingering.TYPE_PHOSPHORUS);
		fire.setPosition(grenade.posX, grenade.posY, grenade.posZ);
		world.spawnEntity(fire);
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					BlockPos bp = new BlockPos((int) Math.floor(grenade.posX) + dx, (int) Math.floor(grenade.posY) + dy, (int) Math.floor(grenade.posZ) + dz);
					if (!world.isAirBlock(bp)) continue;
					for (EnumFacing dir : EnumFacing.values()) {
						BlockPos np = bp.offset(dir);
						if (world.getBlockState(np).getBlock().isFlammable(world, np, dir.getOpposite())) {
							world.setBlockState(bp, Blocks.FIRE.getDefaultState());
							break;
						}
					}
				}
			}
		}
		for (int i = 0; i < 3; i++) {
			PacketThreading.createAllAroundThreadedPacket(
					new AuxParticlePacketNT(HbmEffectNT.Haze, null,
							grenade.posX + world.rand.nextGaussian() * 4,
							grenade.posY,
							grenade.posZ + world.rand.nextGaussian() * 4),
					new TargetPoint(grenade.dimension, grenade.posX, grenade.posY, grenade.posZ, 150));
		}
	};
}
