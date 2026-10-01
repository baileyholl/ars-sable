package com.hollingsworth.ars_sable.common.registry;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.item.MiniatureSublevelData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, ArsSable.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiniatureSublevelData>> MINIATURE_SUBLEVEL = DATA.register("miniature_sublevel",
            () -> DataComponentType.<MiniatureSublevelData>builder().persistent(MiniatureSublevelData.CODEC).networkSynchronized(MiniatureSublevelData.STREAM_CODEC).build());
}
