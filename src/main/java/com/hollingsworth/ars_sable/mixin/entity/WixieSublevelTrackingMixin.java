package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.FlyingMobHelpers;
import com.hollingsworth.arsnouveau.common.entity.EntityWixie;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityWixie.class)
public class WixieSublevelTrackingMixin {
    @Shadow
    public BlockPos cauldronPos;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ars_sable$trackCauldronSublevel(CallbackInfo ci) {
        FlyingMobHelpers.trackAnchorSublevel((EntityWixie) (Object) this, cauldronPos);
    }
}
