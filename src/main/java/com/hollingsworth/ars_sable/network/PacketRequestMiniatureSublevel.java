package com.hollingsworth.ars_sable.network;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.MiniatureSublevelStore;
import com.hollingsworth.arsnouveau.common.network.AbstractPacket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestMiniatureSublevel extends AbstractPacket {
    public static final Type<PacketRequestMiniatureSublevel> TYPE = new Type<>(ArsSable.prefix("request_miniature_sublevel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestMiniatureSublevel> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, packet -> packet.id,
            PacketRequestMiniatureSublevel::new
    );

    public final UUID id;

    public PacketRequestMiniatureSublevel(UUID id) {
        this.id = id;
    }

    @Override
    public void onServerReceived(MinecraftServer minecraftServer, ServerPlayer player) {
        MiniatureSublevelStore.Entry entry = MiniatureSublevelStore.from(player.serverLevel()).get(id);
        if (entry != null) {
            ACNetworking.sendToPlayerClient(new PacketMiniatureSublevelTemplate(id, entry.template()), player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
