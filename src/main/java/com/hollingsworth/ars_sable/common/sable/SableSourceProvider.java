package com.hollingsworth.ars_sable.common.sable;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class SableSourceProvider implements ISpecialSourceProvider {

    ISourceTile tile;
    Level level;
    Supplier<BlockPos> currentPos;

    public SableSourceProvider(ISourceTile tile, Supplier<BlockPos> currentPos, Level level) {
        this.tile = tile;
        this.level = level;
        this.currentPos = currentPos;
    }

    @Override
    public ISourceTile getSource() {
        return tile;
    }

    @Override
    public boolean isValid() {
        if (tile instanceof BlockEntity blockEntity) {
            return Sable.HELPER.getContaining(blockEntity) != null;
        }
        return false;
    }

    @Override
    public BlockPos getCurrentPos() {
        return BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, currentPos.get().getCenter()));
    }
}
