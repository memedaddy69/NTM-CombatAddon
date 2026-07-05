package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.I18nUtil;
import com.hbm.util.Tuple.Quartet;
import com.memedaddy.ntmcombat.api.IExtendedResistanceStats;
import com.memedaddy.ntmcombat.util.AddonDamageHelper;
import com.memedaddy.ntmcombat.util.AddonDamageState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(value = DamageResistanceHandler.class, remap = false)
public class MixinDamageCalculations {

    @Inject(method = "calculateDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void overrideNonSednaDamage(EntityLivingBase entity, DamageSource damage, float amount, float pierceDT, float pierce, CallbackInfoReturnable<Float> cir) {

        // RULE 1: Did a player cause this?
        boolean isPlayer = damage.getTrueSource() instanceof EntityPlayer;

        // RULE 2: Is this currently being routed through HBM's custom NT attack method?
        boolean isNT = AddonDamageState.isNTDamage.get();

        // If a Player caused it, OR it's a SEDNA weapon/turret/hazard, back out and let HBM handle it
        if (isPlayer || isNT) {
            return;
        }

        // --- PIPELINE BEYOND THIS POINT IS STRICTLY MOB/ENVIRONMENT NON-SEDNA ---

        // Absolute damage bypass
        if (damage.isDamageAbsolute()) {
            cir.setReturnValue(amount);
            return;
        }

        Quartet<Item, Item, Item, Item> wornSet = new Quartet<>(
                entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty() ? null : entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem(),
                entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty() ? null : entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem(),
                entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS).isEmpty() ? null : entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem(),
                entity.getItemStackFromSlot(EntityEquipmentSlot.FEET).isEmpty() ? null : entity.getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem()
        );

        // Fixed the Accessor call here
        DamageResistanceHandler.ResistanceStats stats = IDamageResistanceAccessor.getSetStats().get(wornSet);

        if (stats != null) {
            IExtendedResistanceStats extStats = (IExtendedResistanceStats) stats;
            float gdt = extStats.getGDT();
            float gdm = extStats.getGDM();

            // Step 1: Fixed GDT Subtraction (Ignores DT, Unblockable check removed per your rules)
            amount -= gdt;
            if (amount < 1.0F) {
                cir.setReturnValue(0.0F);
                return;
            }

            // Step 2: Elemental DR (Only applies if FIRE, EXPL, or EN)
            float elementalDR = AddonDamageHelper.getElementalDR(stats, damage);
            if (elementalDR > 0F) {
                // Applying armor piercing strictly to the elemental DR multiplier
                float effectiveDR = elementalDR * MathHelper.clamp(1F - pierce, 0F, 2F);
                amount *= (1.0F - effectiveDR);
            }

            // Step 3: General Damage Modifier (GDM)
            if (gdm > 0F) {
                amount *= gdm;
            }
        }

        // Return the final calculation to be capped by HDC later
        cir.setReturnValue(amount);
    }

    @Inject(method = "addInfo", at = @At("TAIL"), remap = false)
    private static void injectCustomTooltips(ItemStack stack, List desc, CallbackInfo ci) {
        if (stack == null || stack.getItem() == null) return;

        Item item = stack.getItem();
        HashMap<Item, List<Quartet<Item, Item, Item, Item>>> itemInfoSet = IDamageResistanceAccessor.getItemInfoSet();

        if (itemInfoSet.containsKey(item)) {
            List<Quartet<Item, Item, Item, Item>> sets = itemInfoSet.get(item);
            if (!sets.isEmpty()) {
                Quartet<Item, Item, Item, Item> set = sets.get(0);
                DamageResistanceHandler.ResistanceStats stats = IDamageResistanceAccessor.getSetStats().get(set);
                if (stats != null) {
                    IExtendedResistanceStats extStats = (IExtendedResistanceStats) stats;
                    List<String> toAdd = new ArrayList<>();
                    float gdt = extStats.getGDT();
                    float gdm = extStats.getGDM();
                    float hdc = extStats.getHDC();

                    if (gdt != 0F)
                        toAdd.add(I18nUtil.resolveKey("damage.gdt") + ": " + gdt);
                    if (gdm != 0F)
                        toAdd.add(I18nUtil.resolveKey("damage.gdm") + ": x" + gdm);
                    if (hdc != 0F)
                        toAdd.add(I18nUtil.resolveKey("damage.hdc") + ": " + hdc);

                    if (!toAdd.isEmpty()) {
                        desc.addAll(toAdd);
                    }
                }
            }
        }
    }
}
