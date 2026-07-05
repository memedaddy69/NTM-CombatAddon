package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.util.DamageResistanceHandler;
import com.hbm.items.ModItems;
import com.hbm.util.Tuple.Quartet;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.HashMap;
import java.util.List;

@Mixin(value = DamageResistanceHandler.class, remap = false)
public interface IDamageResistanceAccessor {

    @Accessor(value = "setStats")
    static HashMap<Quartet<Item, Item, Item, Item>, DamageResistanceHandler.ResistanceStats> getSetStats() {
        throw new AssertionError();
    }

    @Accessor(value = "itemInfoSet")
    static HashMap<Item, List<Quartet<Item, Item, Item, Item>>> getItemInfoSet() {
        throw new AssertionError();
    }
}
