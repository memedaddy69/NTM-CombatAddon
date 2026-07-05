package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.util.DamageResistanceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.HashMap;

@Mixin(DamageResistanceHandler.ResistanceStats.class)
public interface IResistanceStatsAccessor {
    @Accessor(value = "exactResistances", remap = false)
    HashMap<String, DamageResistanceHandler.Resistance> getExactResistances();

    @Accessor(value = "categoryResistances", remap = false)
    HashMap<String, DamageResistanceHandler.Resistance> getCategoryResistances();

    @Accessor(value = "otherResistance", remap = false)
    DamageResistanceHandler.Resistance getOtherResistance();
}
