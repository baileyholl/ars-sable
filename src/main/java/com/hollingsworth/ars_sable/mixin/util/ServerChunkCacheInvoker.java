package com.hollingsworth.ars_sable.mixin.util;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheInvoker {
    @Invoker("clearCache")
    void ars_sable$clearCache();
}
