package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MobJarHelpers {

    public static Vec3 project(Level level, Vec3 pos) {
        return level == null ? pos : Sable.HELPER.projectOutOfSubLevel(level, pos);
    }

    public static boolean isInSublevel(MobJarTile tile) {
        return tile.getLevel() != null && Sable.HELPER.getContaining(tile.getLevel(), tile.getBlockPos().getCenter()) != null;
    }
}
