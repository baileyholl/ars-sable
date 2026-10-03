package com.hollingsworth.ars_sable.common.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record MiniatureSublevelData(UUID id, int blockCount, Vec3i size) {
    public static final Codec<MiniatureSublevelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(MiniatureSublevelData::id),
            Codec.INT.fieldOf("block_count").forGetter(MiniatureSublevelData::blockCount),
            Vec3i.CODEC.fieldOf("size").forGetter(MiniatureSublevelData::size)
    ).apply(instance, MiniatureSublevelData::new));

    public static final StreamCodec<ByteBuf, MiniatureSublevelData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MiniatureSublevelData::id,
            ByteBufCodecs.VAR_INT, MiniatureSublevelData::blockCount,
            ByteBufCodecs.fromCodec(Vec3i.CODEC), MiniatureSublevelData::size,
            MiniatureSublevelData::new
    );
}
