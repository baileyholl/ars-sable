package com.hollingsworth.ars_creo.mixin.planarium;

import com.hollingsworth.ars_creo.common.helper.PlanariumHelpers;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlanariumTile.class)
public class PlanariumTileMixin implements BlockEntitySubLevelActor {
    @WrapMethod(method="sendEntityTo")
    public void sendEntityTo(Entity entity, Operation<Void> original){
        PlanariumHelpers.sendEntityTo((PlanariumTile) (Object)this, entity, original);
    }


}
