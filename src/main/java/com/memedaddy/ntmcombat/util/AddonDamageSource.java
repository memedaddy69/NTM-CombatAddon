package com.memedaddy.ntmcombat.util;

import com.hbm.items.weapon.sedna.DamageSourceSednaNoAttacker;
import com.hbm.items.weapon.sedna.DamageSourceSednaWithAttacker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

public class AddonDamageSource {

    public static DamageSource artillery(Entity projectile, EntityLivingBase shooter) {
        if (shooter != null) {
            return new DamageSourceSednaWithAttacker("artillery", projectile, shooter).setExplosion();
        }
        return new DamageSourceSednaNoAttacker("artillery").setExplosion();
    }

    public static DamageSource himars(Entity projectile, EntityLivingBase shooter) {
        if (shooter != null) {
            return new DamageSourceSednaWithAttacker("himars", projectile, shooter).setExplosion();
        }
        return new DamageSourceSednaNoAttacker("himars").setExplosion();
    }
}
