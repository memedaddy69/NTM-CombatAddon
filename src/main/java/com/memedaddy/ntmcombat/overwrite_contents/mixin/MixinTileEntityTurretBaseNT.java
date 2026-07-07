package com.memedaddy.ntmcombat.overwrite_contents.mixin;

import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.memedaddy.ntmcombat.util.AddonDamageUtil;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityTurretBaseNT.class, remap = false)
public abstract class MixinTileEntityTurretBaseNT {

    // Shadow the field so the mixin can modify it
    @Shadow public Entity target;

    @Redirect(
            method = "seekNewTarget",
            at = @At(value = "FIELD", target = "Lcom/hbm/tileentity/turret/TileEntityTurretBaseNT;target:Lnet/minecraft/entity/Entity;", opcode = org.objectweb.asm.Opcodes.PUTFIELD)
    )
    private void redirectTargetAssignment(TileEntityTurretBaseNT instance, Entity target) {
        instance.target = AddonDamageUtil.unwrapMultiPart(target);
    }
}
