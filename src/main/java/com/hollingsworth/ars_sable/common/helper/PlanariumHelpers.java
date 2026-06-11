package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.ars_sable.common.SublevelPosData;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class PlanariumHelpers {

    public static JarDimData.RotPos onBoundaryBreak(JarDimData instance, UUID uuid, Operation<JarDimData.RotPos> original, Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return original.call(instance, uuid);
        }
        SublevelPosData sublevelPosData = SublevelPosData.from(serverLevel);
        var entry = sublevelPosData.getForPlayer(player.getUUID());
        if (entry == null) {
            return original.call(instance, uuid);
        }
        GlobalPos globalPos = sublevelPosData.getTransformedPos(serverLevel, player);
        if (globalPos == null) {
            return original.call(instance, uuid);
        }
        JarDimData.RotPos enteredFrom = entry.rotPos();
        return new JarDimData.RotPos(globalPos, enteredFrom.rot());
    }


    public static void sendEntityTo(PlanariumTile tile, Entity entity, Operation<Void> original) {
        if (!(tile.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        SubLevelAccess entitySublevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);
        if (entitySublevel == null) {
            entitySublevel = SableCompanion.INSTANCE.getLastTrackingSubLevel(entity);
        }
        BlockPos floorPos;
        GlobalPos fallbackPos;
        if (entitySublevel != null) {
            floorPos = BlockPos.containing(entitySublevel.logicalPose().transformPositionInverse(entity.position())).below();
            fallbackPos = GlobalPos.of(serverLevel.dimension(), SableProjectionHelper.projectStandingPos(serverLevel, floorPos));
        } else {
            floorPos = entity.blockPosition().below();
            fallbackPos = GlobalPos.of(serverLevel.dimension(), entity.blockPosition());
        }
        SublevelPosData.from(serverLevel).put(serverLevel, entity.getUUID(), GlobalPos.of(serverLevel.dimension(), floorPos), entity.getRotationVector(), fallbackPos);
        original.call(entity);
    }
}
