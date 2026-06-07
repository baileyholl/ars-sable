package com.hollingsworth.ars_sable.mixin.pathnavigate;

import com.hollingsworth.ars_sable.common.helper.PathNavigateHelpers;
import com.hollingsworth.arsnouveau.common.entity.EntityBookwyrm;
import com.hollingsworth.arsnouveau.common.entity.goal.bookwyrm.RandomStorageVisitGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RandomStorageVisitGoal.class)
public class BookwyrmRandomStorageVisitGoalMixin {
    @Shadow
    public EntityBookwyrm bookwyrm;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"))
    private boolean ars_sable$moveToProjectedTarget(PathNavigation navigation, double x, double y, double z, double speed, Operation<Boolean> original) {
        Vec3 projected = PathNavigateHelpers.projectTarget(bookwyrm.level(), x, y, z);
        return original.call(navigation, projected.x, projected.y, projected.z, speed);
    }
}
