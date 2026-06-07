package com.hollingsworth.ars_sable.mixin.mob_jar;

import com.hollingsworth.ars_sable.common.helper.MobJarHelpers;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobJarTile.class)
public class MobJarTileMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ars_sable$keepSublevelJarVisible(CallbackInfo ci) {
        MobJarTile tile = (MobJarTile) (Object) this;
        if (tile.cachedEntity != null && MobJarHelpers.isInSublevel(tile)) {
            tile.isVisible = true;
        }
    }

    @WrapOperation(method = {"setEntityData", "writeSimple", "getEntity", "onDispel"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void ars_sable$projectJarEntityPos(Entity entity, double x, double y, double z, Operation<Void> original) {
        Vec3 projected = MobJarHelpers.project(((MobJarTile) (Object) this).getLevel(), new Vec3(x, y, z));
        original.call(entity, projected.x, projected.y, projected.z);
    }
}
