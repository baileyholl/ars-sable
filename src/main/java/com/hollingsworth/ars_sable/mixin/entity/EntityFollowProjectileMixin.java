package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.FollowProjectileHelpers;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityFollowProjectile.class)
public class EntityFollowProjectileMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void ars_sable$projectSublevelTarget(Level level, Vec3 from, Vec3 to, CallbackInfo ci) {
        FollowProjectileHelpers.projectSublevelTarget((EntityFollowProjectile) (Object) this, level, from, to);
    }
}
