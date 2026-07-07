package com.memedaddy.ntmcombat;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.items.ModItems;
import com.hbm.util.Tuple.Quartet;
import com.memedaddy.ntmcombat.api.IExtendedResistanceStats;
import com.memedaddy.ntmcombat.overwrite_contents.mixin.IDamageResistanceAccessor;
import com.memedaddy.ntmcombat.handler.HDCHandler;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import java.util.HashMap;

@Mod(
        modid = "ntmcombat",
        name = "NTM Combat Addon",
        version = "1.0.0",
        dependencies = "required-after:hbm;"
)
public class NTMCombatAddon {

    @Mod.Instance
    public static NTMCombatAddon instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register your HDC Event Handler here
        MinecraftForge.EVENT_BUS.register(new HDCHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Grab the static map once via the Accessor
        HashMap<Quartet<Item, Item, Item, Item>, DamageResistanceHandler.ResistanceStats> setStats = IDamageResistanceAccessor.getSetStats();

        // --- INJECT CUSTOM STATS: GDM / GDT / HDC ---

        // Tier: High GDM, Low/No HDC
        injectStats(setStats, ModItems.steel_helmet, ModItems.steel_plate, ModItems.steel_legs, ModItems.steel_boots, 0.85F, 2.0F, 0.0F);
        injectStats(setStats, ModItems.titanium_helmet, ModItems.titanium_plate, ModItems.titanium_legs, ModItems.titanium_boots, 0.8F, 3.0F, 0.0F);
        injectStats(setStats, ModItems.cobalt_helmet, ModItems.cobalt_plate, ModItems.cobalt_legs, ModItems.cobalt_boots, 0.75F, 2.0F, 0.0F);
        injectStats(setStats, ModItems.envsuit_helmet, ModItems.envsuit_plate, ModItems.envsuit_legs, ModItems.envsuit_boots, 0.75F, 3.0F, 0.0F); // M1TTY

        // Tier: Mid GDM, High HDC
        injectStats(setStats, ModItems.starmetal_helmet, ModItems.starmetal_plate, ModItems.starmetal_legs, ModItems.starmetal_boots, 0.5F, 3.0F, 25.0F);
        injectStats(setStats, ModItems.t51_helmet, ModItems.t51_plate, ModItems.t51_legs, ModItems.t51_boots, 0.5F, 0.0F, 20.0F);
        injectStats(setStats, ModItems.security_helmet, ModItems.security_plate, ModItems.security_legs, ModItems.security_boots, 0.5F, 5.0F, 0.0F);
        injectStats(setStats, ModItems.cmb_helmet, ModItems.cmb_plate, ModItems.cmb_legs, ModItems.cmb_boots, 0.5F, 5.0F, 18.0F);
        injectStats(setStats, ModItems.steamsuit_helmet, ModItems.steamsuit_plate, ModItems.steamsuit_legs, ModItems.steamsuit_boots, 0.5F, 3.0F, 25.0F);
        injectStats(setStats, ModItems.dieselsuit_helmet, ModItems.dieselsuit_plate, ModItems.dieselsuit_legs, ModItems.dieselsuit_boots, 0.5F, 2.0F, 25.0F);
        injectStats(setStats, ModItems.taurun_helmet, ModItems.taurun_plate, ModItems.taurun_legs, ModItems.taurun_boots, 0.5F, 2.0F, 20.0F);
        injectStats(setStats, ModItems.bismuth_helmet, ModItems.bismuth_plate, ModItems.bismuth_legs, ModItems.bismuth_boots, 0.5F, 2.0F, 20.0F);

        // Tier: Low GDM, Mid HDC
        injectStats(setStats, ModItems.ajr_helmet, ModItems.ajr_plate, ModItems.ajr_legs, ModItems.ajr_boots, 0.25F, 4.0F, 12.0F); // Steel Ranger
        injectStats(setStats, ModItems.ajro_helmet, ModItems.ajro_plate, ModItems.ajro_legs, ModItems.ajro_boots, 0.25F, 4.0F, 12.0F); // Steel Ranger Officer
        injectStats(setStats, ModItems.hev_helmet, ModItems.hev_plate, ModItems.hev_legs, ModItems.hev_boots, 0.2F, 2.0F, 8.0F);
        injectStats(setStats, ModItems.bj_helmet, ModItems.bj_plate, ModItems.bj_legs, ModItems.bj_boots, 0.15F, 4.0F, 6.0F); // Lunar
        injectStats(setStats, ModItems.bj_helmet, ModItems.bj_plate_jetpack, ModItems.bj_legs, ModItems.bj_boots, 0.15F, 4.0F, 6.0F); // Lunar Jetpack
        injectStats(setStats, ModItems.trenchmaster_helmet, ModItems.trenchmaster_plate, ModItems.trenchmaster_legs, ModItems.trenchmaster_boots, 0.15F, 5.0F, 8.0F);

        // Tier: Ultra-Low GDM, Extreme HDC
        injectStats(setStats, ModItems.schrabidium_helmet, ModItems.schrabidium_plate, ModItems.schrabidium_legs, ModItems.schrabidium_boots, 0.1F, 0.0F, 4.0F);
        injectStats(setStats, ModItems.rpa_helmet, ModItems.rpa_plate, ModItems.rpa_legs, ModItems.rpa_boots, 0.1F, 25.0F, 4.0F);
        injectStats(setStats, ModItems.ncrpa_helmet, ModItems.ncrpa_plate, ModItems.ncrpa_legs, ModItems.ncrpa_boots, 0.1F, 25.0F, 4.0F);
        injectStats(setStats, ModItems.fau_helmet, ModItems.fau_plate, ModItems.fau_legs, ModItems.fau_boots, 0.05F, 5.0F, 4.0F);
        injectStats(setStats, ModItems.dnt_helmet, ModItems.dnt_plate, ModItems.dnt_legs, ModItems.dnt_boots, 0.001F, 0.0F, 5.0F);
    }

    /**
     * Helper method to seamlessly inject custom values into an existing HBM armor set.
     */
    private void injectStats(HashMap<Quartet<Item, Item, Item, Item>, DamageResistanceHandler.ResistanceStats> map,
                             Item helmet, Item chest, Item legs, Item boots,
                             float gdm, float gdt, float hdc) {

        // 1. Build the specific armor Quartet identical to how HBM registers it
        Quartet<Item, Item, Item, Item> set = new Quartet<>(helmet, chest, legs, boots);

        // 2. Fetch the corresponding stats object from HBM's internal map
        DamageResistanceHandler.ResistanceStats stats = map.get(set);

        // 3. If the stats object exists, cast it to our Duck Interface and set the variables
        if (stats != null) {
            IExtendedResistanceStats extStats = (IExtendedResistanceStats) stats;
            extStats.setGDM(gdm);
            extStats.setGDT(gdt);
            extStats.setHDC(hdc);
        } else {
            // Optional: Print a warning to the console if an armor set wasn't found in HBM's registry
            System.out.println("[NTM Combat Addon] Warning: Could not find base stats for armor set containing " +
                    (chest != null ? chest.getRegistryName() : "unknown chestpiece") +
                    ". Custom stats were not applied.");
        }
    }
}
