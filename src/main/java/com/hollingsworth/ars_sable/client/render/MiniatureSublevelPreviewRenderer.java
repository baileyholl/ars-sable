package com.hollingsworth.ars_sable.client.render;

import com.hollingsworth.ars_sable.common.item.MiniatureSublevelData;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class MiniatureSublevelPreviewRenderer {
    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || player == null || !player.mayBuild()
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().has(DataComponentRegistry.MINIATURE_SUBLEVEL) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        UseOnContext context = new UseOnContext(player, hand, hit);
        MiniatureSublevelData data = context.getItemInHand().get(DataComponentRegistry.MINIATURE_SUBLEVEL);
        if (data == null) {
            return;
        }
        BlockPos anchor = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(context.getLevel(), Vec3.atCenterOf(context.getClickedPos().relative(context.getClickedFace()))));
        Vec3i size = data.size();
        BoundingBox footprint = BoundingBox.orientBox(anchor.getX(), anchor.getY(), anchor.getZ(), -size.getX() / 2, 0, 0, size.getX(), size.getY(), size.getZ(), context.getHorizontalDirection());
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(event.getPoseStack(), buffer.getBuffer(RenderType.lines()), AABB.of(footprint).move(event.getCamera().getPosition().reverse()), 1, 1, 1, 1);
        buffer.endBatch(RenderType.lines());
    }
}
