package com.hollingsworth.ars_sable.mixin.util;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.jcraft.jorbis.Block;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.common.reflection.qual.Invoke;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockUtil.class)
public class BlockPosUtilMixin {

    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)D")
    private static double as$distanceFrom(Level level, Vec3 start, Vec3 end, Operation<Double> original){
        start = Sable.HELPER.projectOutOfSubLevel(level, start);
        end = Sable.HELPER.projectOutOfSubLevel(level, end);
        return original.call(level, start, end);
    }

    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)D")
    private static double as$distanceFrom(Level level, Vec3 start, BlockPos end, Operation<Double> original){
        start = Sable.HELPER.projectOutOfSubLevel(level, start);
        end = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(end)));
        return original.call(level, start, end);
    }

    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)D")
    private static double as$distanceFrom(Level level, BlockPos start, BlockPos end, Operation<Double> original){
        start = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, start.getCenter()));
        end = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level,  end.getCenter()));
        return original.call(level, start, end);
    }

    @WrapMethod(method = "distanceFromCenter(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)D")
    private static double as$distanceFromCenter(Level level, BlockPos start, BlockPos end, Operation<Double> original){
        start = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, start.getCenter()));
        end = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level,  end.getCenter()));
        return original.call(level, start, end);
    }
}
