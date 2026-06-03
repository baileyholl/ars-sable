package com.hollingsworth.ars_sable.mixin.planarium;

import com.hollingsworth.ars_sable.common.helper.PlanariumHelpers;
import com.hollingsworth.arsnouveau.common.block.DimBoundary;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(DimBoundary.class)
public class DimBoundaryMixin {

    @WrapOperation(
            method = "playerAttemptedBreak",
            at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/world/saved_data/JarDimData;getEnteredFrom(Ljava/util/UUID;)Lcom/hollingsworth/arsnouveau/common/world/saved_data/JarDimData$RotPos;")
    )
    private static JarDimData.RotPos fs_getEnteredFrom(JarDimData instance, UUID uuid, Operation<JarDimData.RotPos> original, Level level, Player player) {
        return PlanariumHelpers.onBoundaryBreak(instance, uuid, original, level, player);
    }
}
