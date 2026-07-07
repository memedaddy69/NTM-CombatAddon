package com.memedaddy.ntmcombat.handler;

import com.memedaddy.ntmcombat.util.AddonDamageUtil;
// Make sure this import matches the actual name of your Accessor interface!
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IDamageResistanceAccessor;
import com.memedaddy.ntmcombat.api.IExtendedResistanceStats;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.Tuple.Quartet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.WeakHashMap;

public class HDCHandler {

    // WeakHashMap automatically clears out dead/disconnected players to prevent memory leaks
    private static final WeakHashMap<EntityPlayer, long[]> hdcTickFloor = new WeakHashMap<>();

    /**
     * Hard Damage Cap (HDC) enforcement.
     * Fires at LOWEST priority so it runs after all other damage modifiers have finished their math.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityDamage(LivingDamageEvent event) {

        // 1. Skip if HBM is currently doing SEDNA/Internal math (caught by your Mixin)
        if (AddonDamageUtil.isNTDamage.get()) return;

        // 2. Only apply HDC to players
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 3. Only apply if a mob or environmental hazard hit them (No PvP)
        if (!isValidDamageSource(event.getSource())) return;

        // Fetch the player's HDC from their worn armor
        float hdc = getHDCFor(player);

        float currentHealth = player.getHealth();
        long[] existingFloor = hdcTickFloor.get(player);

        // Check if there is already a tick floor active for the current world tick
        if (existingFloor != null && existingFloor[0] == player.world.getTotalWorldTime()) {
            float floor = Float.intBitsToFloat((int) existingFloor[1]);
            float maxAllowed = currentHealth - floor;

            if (maxAllowed <= 0F) {
                event.setAmount(0F);
                event.setCanceled(true);
                return;
            }
            if (event.getAmount() > maxAllowed) {
                event.setAmount(maxAllowed);
            }
            return;
        }

        // If no HDC is set on the armor, exit
        if (hdc <= 0F) return;

        // Cap the damage to the HDC value
        if (event.getAmount() > hdc) {
            event.setAmount(hdc);
        }

        // Record the health floor for the rest of this tick
        float floorAfterHit = currentHealth - hdc;
        setHDCFloor(player, floorAfterHit);
    }

    private static boolean isValidDamageSource(DamageSource source) {
        Entity attacker = source.getTrueSource();
        // If it is a player attacking, disable HDC (PvP)
        if (attacker instanceof EntityPlayer) {
            return false;
        }
        // Otherwise, allow it. This covers mobs, null sources (fall damage/fire), and explosions.
        return true;
    }

    public static float getHDCFor(EntityPlayer player) {
        Quartet<Item, Item, Item, Item> wornSet = new Quartet<>(
                player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty() ? null : player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem(),
                player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty() ? null : player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem(),
                player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).isEmpty() ? null : player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem(),
                player.getItemStackFromSlot(EntityEquipmentSlot.FEET).isEmpty() ? null : player.getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem()
        );

        // Ensure this call perfectly matches your Accessor's name
        DamageResistanceHandler.ResistanceStats stats = IDamageResistanceAccessor.getSetStats().get(wornSet);
        if (stats != null) {
            return ((IExtendedResistanceStats) stats).getHDC();
        }
        return 0F;
    }

    public static void setHDCFloor(EntityPlayer player, float healthFloor) {
        long currentTick = player.world.getTotalWorldTime();
        long[] existing = hdcTickFloor.get(player);
        if (existing != null && existing[0] == currentTick) {
            float existingFloor = Float.intBitsToFloat((int) existing[1]);
            if (healthFloor > existingFloor) {
                existing[1] = Float.floatToRawIntBits(healthFloor);
            }
        } else {
            hdcTickFloor.put(player, new long[]{currentTick, Float.floatToRawIntBits(healthFloor)});
        }
    }
    // ADD THIS to HDCHandler.java
    public static float applyHDCFloor(EntityPlayer player, float proposedHealth) {
        long[] existingFloor = hdcTickFloor.get(player);
        if (existingFloor != null && existingFloor[0] == player.world.getTotalWorldTime()) {
            float floor = Float.intBitsToFloat((int) existingFloor[1]);
            // Returns whichever is higher: the proposed health, or the absolute floor
            return Math.max(proposedHealth, floor);
        }
        return proposedHealth;
    }
}
