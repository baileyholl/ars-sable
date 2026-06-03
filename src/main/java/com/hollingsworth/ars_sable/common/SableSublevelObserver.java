package com.hollingsworth.ars_sable.common;

import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.server.level.ServerLevel;

public class SableSublevelObserver implements SubLevelObserver {
    @Override
    public void onSubLevelAdded(SubLevel subLevel) {
        SubLevelObserver.super.onSubLevelAdded(subLevel);
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        SubLevelObserver.super.onSubLevelRemoved(subLevel, reason);
        if(subLevel.getLevel() instanceof ServerLevel serverLevel) {
            if (reason == SubLevelRemovalReason.REMOVED) {
                SublevelPosData.from(serverLevel).removeSublevel(serverLevel, subLevel.getUniqueId());
            }
        }
    }

}
