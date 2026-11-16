package com.hollingsworth.ars_sable.mixin.entity;

import com.hollingsworth.ars_sable.common.helper.SpellProjectileHelpers;
import com.hollingsworth.arsnouveau.api.block.IPrismaticBlock;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityProjectileSpell.class)
public class EntityProjectileSpellMixin {

    @ModifyArg(method = "traceAnyHit", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell;findHitEntity(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/EntityHitResult;"), index = 1)
    private Vec3 ars_sable$projectHitOutOfSublevel(Vec3 nextPosition) {
        return Sable.HELPER.projectOutOfSubLevel(((Entity) (Object) this).level(), nextPosition);
    }

    @WrapOperation(method = "redirect", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/block/IPrismaticBlock;onHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell;)V"))
    private void ars_sable$redirectOnSublevel(IPrismaticBlock prism, Level level, BlockState state, BlockPos pos, EntityProjectileSpell spell, Operation<Void> original) {
        SpellProjectileHelpers.redirectOnSublevel(level, pos, spell, () -> original.call(prism, level, state, pos, spell));
    }
}
