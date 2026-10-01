package com.hollingsworth.ars_sable.common.item;

import com.hollingsworth.ars_sable.common.helper.MiniatureSublevelHelper;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import dev.ryanhcode.sable.Sable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MiniatureSublevelItem extends Item {
    public MiniatureSublevelItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        MiniatureSublevelData data = stack.get(DataComponentRegistry.MINIATURE_SUBLEVEL);
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Vec3 hitLocation = Vec3.atCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        BlockPos anchor = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, hitLocation));

        if (!MiniatureSublevelHelper.place(level, data.id(), anchor, context.getHorizontalDirection(), player)) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        if (player != null && player.hasInfiniteMaterials()) {
            player.setItemInHand(context.getHand(), ItemStack.EMPTY);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        MiniatureSublevelData data = stack.get(DataComponentRegistry.MINIATURE_SUBLEVEL);
        if (data == null) {
            return;
        }
        tooltip.add(Component.translatable("ars_sable.miniature_sublevel.blocks", data.blockCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ars_sable.miniature_sublevel.size", data.size().getX(), data.size().getY(), data.size().getZ()).withStyle(ChatFormatting.GRAY));
    }
}
