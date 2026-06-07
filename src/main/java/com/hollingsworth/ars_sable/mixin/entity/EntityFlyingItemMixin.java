package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.FollowProjectileHelpers;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityFlyingItem.class)
public class EntityFlyingItemMixin {
    @Shadow
    int maxAge;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;III)V", at = @At("TAIL"))
    private void ars_sable$projectSublevelTarget(Level level, Vec3 from, Vec3 to, int r, int g, int b, CallbackInfo ci) {
        EntityFlyingItem item = (EntityFlyingItem) (Object) this;
        FollowProjectileHelpers.projectSublevelTarget(item, level, from, to);
        this.maxAge = (int) Math.floor(item.getEntityData().get(EntityFlyingItem.from).subtract(item.getEntityData().get(EntityFlyingItem.to)).length() * 5);
    }
}
