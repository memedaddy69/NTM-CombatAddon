package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface IEntityLivingBaseAccessor {

    @Accessor("lastDamage")
    float getLastDamage();

    @Accessor("lastDamage")
    void setLastDamage(float damage);

    @Accessor("recentlyHit")
    int getRecentlyHit();

    @Accessor("recentlyHit")
    void setRecentlyHit(int recentlyHit);

    @Accessor("attackingPlayer")
    EntityPlayer getAttackingPlayer();

    @Accessor("attackingPlayer")
    void setAttackingPlayer(EntityPlayer player);

}
