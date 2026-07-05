package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.memedaddy.ntmcombat.api.IExtendedResistanceStats;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.util.DamageResistanceHandler;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageResistanceHandler.ResistanceStats.class)
public class MixinResistanceStats implements IExtendedResistanceStats {

    @Unique private float addon_gdt = 0F;
    @Unique private float addon_gdm = 0F;
    @Unique private float addon_hdc = 0F;

    @Override public float getGDT() { return this.addon_gdt; }
    @Override public void setGDT(float gdt) { this.addon_gdt = gdt; }
    @Override public float getGDM() { return this.addon_gdm; }
    @Override public void setGDM(float gdm) { this.addon_gdm = gdm; }
    @Override public float getHDC() { return this.addon_hdc; }
    @Override public void setHDC(float hdc) { this.addon_hdc = hdc; }

    /**
     * Intercept HBM's JSON reader to inject GDT/GDM/HDC from the config.
     */
    @Inject(method = "deserialize", at = @At("RETURN"), remap = false)
    private static void injectCustomJsonParsing(JsonObject obj, CallbackInfoReturnable<DamageResistanceHandler.ResistanceStats> cir) {

        DamageResistanceHandler.ResistanceStats stats = cir.getReturnValue();

        if (stats != null) {
            IExtendedResistanceStats extStats = (IExtendedResistanceStats) stats;

            if (obj.has("gdt")) {
                extStats.setGDT(obj.get("gdt").getAsFloat());
            }
            if (obj.has("gdm")) {
                extStats.setGDM(obj.get("gdm").getAsFloat());
            }
            if (obj.has("hdc")) {
                extStats.setHDC(obj.get("hdc").getAsFloat());
            }
        }
    }

    /**
     * Intercept HBM's JSON writer so GDT/GDM/HDC are persisted to disk.
     */
    @Inject(method = "serialize", at = @At("TAIL"), remap = false)
    private void injectCustomSerialization(JsonWriter writer, CallbackInfo ci) throws IOException {
        if (addon_gdt != 0F) writer.name("gdt").value(addon_gdt);
        if (addon_gdm != 0F) writer.name("gdm").value(addon_gdm);
        if (addon_hdc != 0F) writer.name("hdc").value(addon_hdc);
    }
}
