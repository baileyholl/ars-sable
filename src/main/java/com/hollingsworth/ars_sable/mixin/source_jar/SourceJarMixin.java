package com.hollingsworth.ars_sable.mixin.source_jar;

import com.hollingsworth.ars_sable.common.sable.SableSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.ITickable;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SourceJarTile.class)
public abstract class SourceJarMixin implements BlockEntitySubLevelActor {

    SableSourceProvider sableSourceProvider;

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if(sableSourceProvider == null || !sableSourceProvider.isValid()){
            SourceJarTile sourceJarTile = (SourceJarTile)(Object)this;
            sableSourceProvider = new SableSourceProvider(sourceJarTile, sourceJarTile::getBlockPos, subLevel.getLevel());
            SourceManager.INSTANCE.addInterface(sourceJarTile.getLevel(), sableSourceProvider);
        }
    }
}
