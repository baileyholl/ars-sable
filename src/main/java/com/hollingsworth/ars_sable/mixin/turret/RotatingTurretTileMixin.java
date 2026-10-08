package com.hollingsworth.ars_sable.mixin.turret;

import com.hollingsworth.arsnouveau.common.block.tile.RotatingTurretTile;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RotatingTurretTile.class)
public class RotatingTurretTileMixin {

    @ModifyVariable(method = "aim", at = @At("STORE"), ordinal = 1)
    private Vec3 ars_sable$aimInTurretFrame(Vec3 target) {
        BlockEntity tile = (BlockEntity) (Object) this;
        BlockPos framePos = tile.getBlockPos();
        Level level = tile.getLevel();
        SubLevel frame = Sable.HELPER.getContaining(level, framePos);
        if (frame == Sable.HELPER.getContaining(level, target)) {
            return target;
        }
        Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(level, target);
        return frame == null ? worldPos : frame.logicalPose().transformPositionInverse(worldPos);
    }
}
