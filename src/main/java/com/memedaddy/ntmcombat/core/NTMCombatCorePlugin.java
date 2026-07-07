package com.memedaddy.ntmcombat.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("NTMCombatCorePlugin")
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE + 1000)
public class NTMCombatCorePlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public NTMCombatCorePlugin() {
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ntmcombat.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.memedaddy.ntmcombat.core.NTMCombatTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
