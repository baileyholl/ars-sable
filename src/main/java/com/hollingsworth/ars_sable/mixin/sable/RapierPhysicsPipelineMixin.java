package com.hollingsworth.ars_sable.mixin.sable;

import com.hollingsworth.ars_sable.common.block.ImpulsePrismBlock;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")
public class RapierPhysicsPipelineMixin {
    @WrapMethod(method = "applyImpulse")
    private void ars_sable$redirectPrismImpulse(PhysicsPipelineBody body, Vector3dc position, Vector3dc force, Operation<Void> original) {
        if (body instanceof ServerSubLevel subLevel && force.lengthSquared() > 0) {
            Vector3d point = new Vector3d(force).normalize(1.0E-4).add(position);
            BlockPos pos = BlockPos.containing(point.x, point.y, point.z);
            BlockState state = subLevel.getLevel().getBlockState(pos);
            if (state.getBlock() instanceof ImpulsePrismBlock prism) {
                prism.redirectImpulse(subLevel.getLevel(), pos, state, force);
                return;
            }
        }
        original.call(body, position, force);
    }
}
