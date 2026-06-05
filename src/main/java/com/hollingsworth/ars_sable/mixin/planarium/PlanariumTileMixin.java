package com.hollingsworth.ars_sable.mixin.planarium;

import com.hollingsworth.ars_sable.common.helper.PlanariumHelpers;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlanariumTile.class)
public class PlanariumTileMixin {
    @WrapMethod(method="sendEntityTo")
    public void sendEntityTo(Entity entity, Operation<Void> original){
        PlanariumHelpers.sendEntityTo((PlanariumTile) (Object)this, entity, original);
    }
}
