package com.hollingsworth.ars_sable.network;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.client.render.MiniatureSublevelItemRenderer;
import com.hollingsworth.arsnouveau.common.network.AbstractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketMiniatureSublevelTemplate extends AbstractPacket {
    public static final Type<PacketMiniatureSublevelTemplate> TYPE = new Type<>(ArsSable.prefix("miniature_sublevel_template"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketMiniatureSublevelTemplate> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, packet -> packet.id,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, packet -> packet.template,
            PacketMiniatureSublevelTemplate::new
    );

    public final UUID id;
    public final CompoundTag template;

    public PacketMiniatureSublevelTemplate(UUID id, CompoundTag template) {
        this.id = id;
        this.template = template;
    }

    @Override
    public void onClientReceived(Minecraft minecraft, Player player) {
        MiniatureSublevelItemRenderer.accept(id, template);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
