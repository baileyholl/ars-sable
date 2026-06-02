package com.hollingsworth.ars_creo.common.block;

import com.hollingsworth.ars_creo.CreoConfig;
import com.hollingsworth.ars_creo.common.registry.ModBlockRegistry;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class StarbuncleWheelTile extends GeneratingKineticBlockEntity implements GeoBlockEntity {
    @SuppressWarnings("all")
    private AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    protected float generatedSpeed = 0;

    public StarbuncleWheelTile(BlockPos pos, BlockState state) {
        super(ModBlockRegistry.STARBY_TILE.get(), pos, state);
    }

    public void findGoldBlock() {
        Direction direction = getBlockState().getValue(StarbuncleWheelBlock.FACING);
        generatedSpeed = direction != Direction.UP && direction != Direction.DOWN && level != null &&
                level.getBlockState(getBlockPos().relative(direction.getClockWise())).is(Tags.Blocks.STORAGE_BLOCKS_GOLD)
                ? CreoConfig.WHEEL_BONUS_SPEED.get() : CreoConfig.WHEEL_BASE_SPEED.get();
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(StarbuncleWheelBlock.FACING));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        findGoldBlock();
        updateGeneratedRotation();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 1, (s) ->{
            s.getController().setAnimation(RawAnimation.begin().thenPlay("run"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }
}