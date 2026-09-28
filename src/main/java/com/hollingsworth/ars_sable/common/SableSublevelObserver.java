package com.hollingsworth.ars_sable.common;

import com.hollingsworth.arsnouveau.api.event.EventQueue;
import com.hollingsworth.arsnouveau.api.event.ITimedEvent;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.server.level.ServerLevel;

public class SableSublevelObserver implements SubLevelObserver {
    @Override
    public void onSubLevelAdded(SubLevel subLevel) {
        SubLevelObserver.super.onSubLevelAdded(subLevel);
        if (subLevel.getLevel() instanceof ServerLevel serverLevel) {
            runWhenOverworldLoaded(serverLevel, () -> {
                SublevelPosData.from(serverLevel).setSublevelLoaded(serverLevel, subLevel.getUniqueId(), true);
                WarpSublevelTargetData.from(serverLevel).setSublevelLoaded(serverLevel, subLevel.getUniqueId(), true);
            });
        }
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        SubLevelObserver.super.onSubLevelRemoved(subLevel, reason);
        if (subLevel.getLevel() instanceof ServerLevel serverLevel) {
            if (reason == SubLevelRemovalReason.REMOVED) {
                runWhenOverworldLoaded(serverLevel, () -> {
                    SublevelPosData.from(serverLevel).removeSublevel(serverLevel, subLevel.getUniqueId());
                    WarpSublevelTargetData.from(serverLevel).removeSublevel(serverLevel, subLevel.getUniqueId());
                });
            } else if (reason == SubLevelRemovalReason.UNLOADED) {
                runWhenOverworldLoaded(serverLevel, () -> {
                    SublevelPosData.from(serverLevel).setSublevelLoaded(serverLevel, subLevel.getUniqueId(), false);
                    WarpSublevelTargetData.from(serverLevel).setSublevelLoaded(serverLevel, subLevel.getUniqueId(), false);
                });
            }
        }
    }

    // Sable loads sublevels before the overworld is loaded, deferring this prevents NPE with the overworld data.
    public static void runWhenOverworldLoaded(ServerLevel level, Runnable action) {
        if (level.getServer().overworld() != null) {
            action.run();
            return;
        }
        EventQueue.getServerInstance().addEvent(new WaitForOverworldEvent(level, action));
    }

    public static class WaitForOverworldEvent implements ITimedEvent {
        private final ServerLevel level;
        private final Runnable action;
        private boolean done;

        public WaitForOverworldEvent(ServerLevel level, Runnable action) {
            this.level = level;
            this.action = action;
        }

        @Override
        public void tick(boolean serverSide) {
            if (!done && level.getServer().overworld() != null) {
                done = true;
                action.run();
            }
        }

        @Override
        public boolean isExpired() {
            return done;
        }
    }
}
