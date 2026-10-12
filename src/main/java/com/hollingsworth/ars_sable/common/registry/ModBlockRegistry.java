package com.hollingsworth.ars_sable.common.registry;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.block.ImpulsePrismBlock;
import com.hollingsworth.ars_sable.common.item.MiniatureSublevelItem;
import com.hollingsworth.ars_sable.common.lib.LibBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockRegistry {
    public static final DeferredRegister<Block> BLOCK_REG = DeferredRegister.create(BuiltInRegistries.BLOCK, ArsSable.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsSable.MODID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ArsSable.MODID);

    public static final DeferredHolder<Item, MiniatureSublevelItem> MINIATURE_SUBLEVEL = ITEMS.register("miniature_sublevel", () -> new MiniatureSublevelItem(new Item.Properties().fireResistant().stacksTo(1)));

    public static final DeferredHolder<Block, ImpulsePrismBlock> IMPULSE_PRISM = BLOCK_REG.register(LibBlock.IMPULSE_PRISM, ImpulsePrismBlock::new);
    public static final DeferredHolder<Item, BlockItem> IMPULSE_PRISM_ITEM = ITEMS.register(LibBlock.IMPULSE_PRISM, () -> new BlockItem(IMPULSE_PRISM.get(), new Item.Properties()));


    public static void onBlockItemsRegistry() {

    }

    public static Block.Properties defaultProperties() {
        return Block.Properties.of().sound(SoundType.STONE).strength(2.0f, 6.0f);
    }
}