package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.FlyingMobHelpers;
import com.hollingsworth.arsnouveau.common.entity.Whirlisprig;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Whirlisprig.class)
public class WhirlisprigSublevelTrackingMixin {
    @Shadow
    public BlockPos flowerPos;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ars_sable$trackFlowerSublevel(CallbackInfo ci) {
        FlyingMobHelpers.trackAnchorSublevel((Whirlisprig) (Object) this, flowerPos);
    }
}
