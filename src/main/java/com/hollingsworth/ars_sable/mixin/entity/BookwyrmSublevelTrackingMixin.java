package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.BookwyrmHelpers;
import com.hollingsworth.arsnouveau.common.entity.EntityBookwyrm;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityBookwyrm.class)
public class BookwyrmSublevelTrackingMixin {
    @Shadow
    public BlockPos lecternPos;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ars_sable$trackLecternSublevel(CallbackInfo ci) {
        BookwyrmHelpers.trackLecternSublevel((EntityBookwyrm) (Object) this, lecternPos);
    }
}
