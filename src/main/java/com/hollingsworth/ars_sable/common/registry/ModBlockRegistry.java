package com.hollingsworth.ars_sable.common.registry;

import com.hollingsworth.ars_sable.ArsSable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockRegistry {
    public static final DeferredRegister<Block> BLOCK_REG = DeferredRegister.create(BuiltInRegistries.BLOCK, ArsSable.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsSable.MODID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ArsSable.MODID);


    public static void onBlockItemsRegistry() {

    }

    public static Block.Properties defaultProperties(){
        return Block.Properties.of().sound(SoundType.STONE).strength(2.0f, 6.0f);
    }
}