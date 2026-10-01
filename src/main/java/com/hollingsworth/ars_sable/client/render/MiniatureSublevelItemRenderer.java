package com.hollingsworth.ars_sable.client.render;

import com.hollingsworth.ars_sable.common.item.MiniatureSublevelData;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import com.hollingsworth.ars_sable.network.ACNetworking;
import com.hollingsworth.ars_sable.network.PacketRequestMiniatureSublevel;
import com.hollingsworth.arsnouveau.client.renderer.tile.PlanariumRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MiniatureSublevelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static Map<UUID, PlanariumRenderer.StructureRenderData> RENDERS = new HashMap<>();
    private static Set<UUID> REQUESTED = new HashSet<>();

    public MiniatureSublevelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void accept(UUID id, CompoundTag templateTag) {
        if (RENDERS.containsKey(id)) {
            return;
        }
        StructureTemplate template = new StructureTemplate();
        template.load(Minecraft.getInstance().level.holderLookup(Registries.BLOCK), templateTag);
        PlanariumRenderer.StructureRenderData data = new PlanariumRenderer.StructureRenderData(template, 0);
        PlanariumRenderer.generateRender(data, data.fakeRenderingWorld, BlockPos.ZERO, Vec3.ZERO);
        // Not needed for item render
        PlanariumRenderer.clearByteBuffers(data);
        RENDERS.put(id, data);
    }

    public static void clear() {
        RENDERS.values().forEach(data -> data.vertexBuffers.values().forEach(VertexBuffer::close));
        RENDERS.clear();
        REQUESTED.clear();
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        MiniatureSublevelData data = stack.get(DataComponentRegistry.MINIATURE_SUBLEVEL);
        if (data == null) {
            return;
        }
        if (REQUESTED.add(data.id())) {
            ACNetworking.sendToServer(new PacketRequestMiniatureSublevel(data.id()));
        }
        PlanariumRenderer.StructureRenderData renderData = RENDERS.get(data.id());
        if (renderData == null) {
            return;
        }

        Vec3i size = data.size();
        float scale = 1.0f / Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-size.getX() / 2.0, -size.getY() / 2.0, -size.getZ() / 2.0);

        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
        drawBuffers(renderData, poseStack);
        renderBlockEntities(renderData, poseStack, buffer);
        poseStack.popPose();
    }

    private static void drawBuffers(PlanariumRenderer.StructureRenderData renderData, PoseStack poseStack) {
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose())
                .translate(0, -26, 0)
                .scale(1 / PlanariumRenderer.scale)
                .translate(-PlanariumRenderer.offset, -PlanariumRenderer.offset, -PlanariumRenderer.offset);
        for (RenderType renderType : RenderType.chunkBufferLayers()) {
            VertexBuffer vertexBuffer = renderData.vertexBuffers.get(renderType);
            if (vertexBuffer.getFormat() == null) {
                continue;
            }
            try {
                renderType.setupRenderState();
                vertexBuffer.bind();
                vertexBuffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                VertexBuffer.unbind();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                renderType.clearRenderState();
            }
        }
    }

    private static void renderBlockEntities(PlanariumRenderer.StructureRenderData renderData, PoseStack poseStack, MultiBufferSource buffer) {
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for (BlockEntity fakeTile : renderData.fakeRenderingWorld.blockEntityMap.values()) {
            BlockEntityRenderer<BlockEntity> renderer = dispatcher.getRenderer(fakeTile);
            if (renderer == null) {
                continue;
            }
            if (fakeTile instanceof PlanariumTile planariumTile) {
                planariumTile.key = null;
            }
            BlockPos pos = fakeTile.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                renderer.render(fakeTile, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            poseStack.popPose();
        }
    }
}
