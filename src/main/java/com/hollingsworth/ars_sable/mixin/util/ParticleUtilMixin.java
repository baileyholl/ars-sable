package com.hollingsworth.ars_sable.mixin.util;

import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleUtil.class)
public class ParticleUtilMixin {
    @WrapMethod(method = "Lcom/hollingsworth/arsnouveau/client/particle/ParticleUtil;spawnFollowProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lcom/hollingsworth/arsnouveau/client/particle/ParticleColor;)V")
    private static void as$spawnFollowProjectile(Level world, BlockPos from, BlockPos _to, ParticleColor color, Operation<Void> original){
        from = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(world, from.getCenter()));
        _to = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(world,  _to.getCenter()));
        original.call(world, from, _to, color);
    }

    @WrapMethod(method = "Lcom/hollingsworth/arsnouveau/client/particle/ParticleUtil;beam(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)V")
    private static void beam(BlockPos toThisBlock, BlockPos fromThisBlock, Level world, Operation<Void> original) {
        toThisBlock = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(world, toThisBlock.getCenter()));
        fromThisBlock = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(world, fromThisBlock.getCenter()));
        original.call(toThisBlock, fromThisBlock, world);
    }
}
