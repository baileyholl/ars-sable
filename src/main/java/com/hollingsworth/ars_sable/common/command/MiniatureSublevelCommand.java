package com.hollingsworth.ars_sable.common.command;

import com.hollingsworth.ars_sable.common.MiniatureSublevelStore;
import com.hollingsworth.ars_sable.common.helper.MiniatureSublevelHelper;
import com.hollingsworth.ars_sable.common.item.MiniatureSublevelData;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MiniatureSublevelCommand {
    private static final SimpleCommandExceptionType NOT_FOUND = new SimpleCommandExceptionType(Component.translatable("ars_sable.command.miniature.not_found"));
    private static final SuggestionProvider<CommandSourceStack> STORED_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(MiniatureSublevelStore.from(ctx.getSource().getLevel()).getEntries().keySet().stream().map(UUID::toString), builder);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ars-sable")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("miniature")
                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("give")
                                .then(Commands.argument("id", UuidArgument.uuid())
                                        .suggests(STORED_IDS)
                                        .executes(ctx -> give(ctx.getSource(), UuidArgument.getUuid(ctx, "id"), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> give(ctx.getSource(), UuidArgument.getUuid(ctx, "id"), EntityArgument.getPlayer(ctx, "player"))))))));
    }

    private static int list(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Map<UUID, MiniatureSublevelStore.Entry> entries = MiniatureSublevelStore.from(level).getEntries();
        if (entries.isEmpty()) {
            source.sendFailure(Component.translatable("ars_sable.command.miniature.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("ars_sable.command.miniature.list", entries.size()), false);
        GameProfileCache profiles = source.getServer().getProfileCache();
        entries.forEach((id, entry) -> {
            MiniatureSublevelData data = MiniatureSublevelHelper.createItem(level, id, entry).get(DataComponentRegistry.MINIATURE_SUBLEVEL);
            Component label = ComponentUtils.wrapInSquareBrackets(Component.literal(entry.name().orElse(id.toString()))).withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ars-sable miniature give " + id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(id.toString()))));
            Component owner = entry.owner()
                    .map(ownerId -> Optional.ofNullable(profiles).flatMap(cache -> cache.get(ownerId)).map(GameProfile::getName).orElse(ownerId.toString()))
                    .<Component>map(Component::literal)
                    .orElse(Component.translatable("ars_sable.command.miniature.unknown_owner"));
            source.sendSuccess(() -> Component.translatable("ars_sable.command.miniature.entry", label, owner, data.blockCount(),
                    data.size().getX(), data.size().getY(), data.size().getZ(), entry.dimension().location().toString()), false);
        });
        return entries.size();
    }

    private static int give(CommandSourceStack source, UUID id, ServerPlayer player) throws CommandSyntaxException {
        MiniatureSublevelStore.Entry entry = MiniatureSublevelStore.from(source.getLevel()).get(id);
        if (entry == null) {
            throw NOT_FOUND.create();
        }
        ItemStack stack = MiniatureSublevelHelper.createItem(source.getLevel(), id, entry);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable("ars_sable.command.miniature.give", entry.name().orElse(id.toString()), player.getDisplayName()), true);
        return 1;
    }
}
