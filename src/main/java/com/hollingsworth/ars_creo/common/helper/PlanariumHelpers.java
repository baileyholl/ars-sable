package com.hollingsworth.ars_creo.common.helper;

import com.hollingsworth.ars_creo.common.SublevelPosData;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.UUID;

public class PlanariumHelpers {

    public static JarDimData.RotPos onBoundaryBreak(JarDimData instance, UUID uuid, Operation<JarDimData.RotPos> original, Level level, Player player){
        if(!(level instanceof ServerLevel serverLevel)){
            return original.call(instance, uuid);
        }
        SublevelPosData sublevelPosData = SublevelPosData.from(serverLevel);
        var entry = sublevelPosData.getForPlayer(player.getUUID());
        if(entry == null){
            return original.call(instance, uuid);
        }
        ServerLevel originalLevel = serverLevel.getServer().getLevel(entry.rotPos().pos().dimension());
        JarDimData.RotPos enteredFrom = entry.rotPos();

        Vec3 realTilePos = SableCompanion.INSTANCE.projectOutOfSubLevel(originalLevel, (Position) entry.localSublevelPos().getCenter());
        BlockPos enteredFromOffset = enteredFrom.pos().pos();
        BlockPos newPos = BlockPos.containing(realTilePos.add(Vec3.atLowerCornerOf(enteredFromOffset)));
        return new JarDimData.RotPos(new GlobalPos(enteredFrom.pos().dimension(), newPos), enteredFrom.rot());
    }

    public static void sendEntityTo(PlanariumTile tile, Entity entity, Operation<Void> original){
        if(!(tile.getLevel() instanceof ServerLevel serverLevel)){
            return;
        }
        SubLevelAccess entitySublevel = SableCompanion.INSTANCE.getLastTrackingSubLevel(entity);
        if(entitySublevel == null){
            original.call(entity);
            return;
        }
        SubLevelAccess tileSublevel = SableCompanion.INSTANCE.getContaining(tile);
        if(tileSublevel == null){
            original.call(entity);
            return;
        }
        if(!(entity instanceof EntityMovementExtension movementExtension)){
            return;
        }
        BlockPos restorePos = movementExtension.sable$getInBlockStatePos().subtract(tile.getBlockPos());
        SublevelPosData.from(serverLevel).put(entity.getUUID(), tileSublevel.getUniqueId(),tile.getBlockPos(), new JarDimData.RotPos(new GlobalPos(entity.level().dimension(), restorePos), entity.getRotationVector()));
        original.call(entity);
    }
}
