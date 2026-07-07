package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.memedaddy.ntmcombat.handler.HDCHandler;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityPlayer {

    /**
     * Guards against reentrancy from our own clamped setHealth call below.
     */
    private static final ThreadLocal<Boolean> hbm$hdcActive = ThreadLocal.withInitial(() -> false);

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void hbm$enforceHDC(float health, CallbackInfo ci) {

        // Prevent reentrant calls triggered by our own correction below.
        if (hbm$hdcActive.get()) return;

        // Only apply outside of SEDNA/HBM internal damage.
        if (AddonDamageUtil.isNTDamage.get()) return;

        EntityLivingBase living = (EntityLivingBase) (Object) this;
        if (!(living instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) living;

        // Server-side only — client health updates are cosmetic.
        if (player.world.isRemote) return;

        // Only intercept health reductions, not healing.
        float currentHealth = living.getHealth();
        if (health >= currentHealth) return;

        // --- Step 1: check the active per-tick floor first ---
        float floored = HDCHandler.applyHDCFloor(player, health);
        if (floored > health) {
            ci.cancel();
            hbm$hdcActive.set(true);
            try {
                living.setHealth(floored);
            } finally {
                hbm$hdcActive.set(false);
            }
            return;
        }

        // --- Step 2: per-call HDC cap ---
        // Exclude PvP: If the player's last attacker was a player, skip HDC.
        IEntityLivingBaseAccessor accessor = (IEntityLivingBaseAccessor) player;
        if (accessor.getAttackingPlayer() != null && accessor.getRecentlyHit() > 0) return;

        float hdc = HDCHandler.getHDCFor(player);
        if (hdc <= 0F) return;

        float proposedDamage = currentHealth - health;
        if (proposedDamage > hdc) {
            float clampedHealth = currentHealth - hdc;

            // Record the floor so the rest of this tick is protected.
            HDCHandler.setHDCFloor(player, clampedHealth);

            ci.cancel();
            hbm$hdcActive.set(true);
            try {
                living.setHealth(clampedHealth);
            } finally {
                hbm$hdcActive.set(false);
            }
        } else {
            // Hit is within HDC. Record the floor so subsequent sources this tick can't pile on.
            float floorAfterHit = currentHealth - hdc;
            HDCHandler.setHDCFloor(player, floorAfterHit);
        }
    }
}
