package com.memedaddy.ntmcombat.overwrite_contents.mixin;

@Mixin(DamageResistanceHandler.ResistanceStats.class)
public interface IResistanceStatsAccessor {
    @Accessor(value = "exactResistances", remap = false)
    HashMap<String, DamageResistanceHandler.Resistance> getExactResistances();

    @Accessor(value = "categoryResistances", remap = false)
    HashMap<String, DamageResistanceHandler.Resistance> getCategoryResistances();
}
