package com.hollingsworth.ars_creo.network;

import com.hollingsworth.ars_creo.ArsCreo;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.network.AbstractPacket;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;


public class PacketUpdateJarContraption extends AbstractPacket {
    public static final Type<PacketUpdateJarContraption> TYPE = new Type(ArsCreo.prefix("update_jar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateJarContraption> CODEC = StreamCodec.ofMember(PacketUpdateJarContraption::toBytes, PacketUpdateJarContraption::new);

    public int entityID;
    public BlockPos structurePos;
    public CompoundTag structureTag;
    public int fillLevel;

    public PacketUpdateJarContraption(FriendlyByteBuf buf) {
        this.structurePos = buf.readBlockPos();
        this.structureTag = buf.readNbt();
        this.fillLevel = buf.readInt();
        this.entityID = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(structurePos);
        buf.writeNbt(structureTag);
        buf.writeInt(fillLevel);
        buf.writeInt(entityID);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void onClientReceived(Minecraft minecraft, Player player) {
        Entity entity = ArsNouveau.proxy.getClientWorld().getEntity(entityID);
        if(entity instanceof AbstractContraptionEntity contraption){
            contraption.getContraption().getBlocks().put(structurePos,
                    new StructureTemplate.StructureBlockInfo(structurePos, BlockRegistry.SOURCE_JAR.defaultBlockState().setValue(SourceJar.fill, fillLevel), structureTag));
            contraption.getContraption().invalidateClientContraptionStructure();
        }
    }
}
