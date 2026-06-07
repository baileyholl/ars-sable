package com.hollingsworth.ars_sable.mixin.render;

import com.hollingsworth.ars_sable.common.helper.MobJarHelpers;
import com.hollingsworth.arsnouveau.client.renderer.tile.MobJarRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobJarRenderer.class)
public class MobJarRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void ars_sable$shouldRenderSublevelJar(MobJarTile blockEntity, Vec3 cameraPos, CallbackInfoReturnable<Boolean> cir) {
        if (blockEntity.isVisible && MobJarHelpers.isInSublevel(blockEntity)) {
            cir.setReturnValue(true);
        }
    }
}
