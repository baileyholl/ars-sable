package com.hollingsworth.ars_creo.common;

import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;

public class SableSublevelObserver implements SubLevelObserver {
    @Override
    public void onSubLevelAdded(SubLevel subLevel) {
        SubLevelObserver.super.onSubLevelAdded(subLevel);
        System.out.println("new added" + subLevel.getLevel());
        System.out.println(subLevel.getUniqueId());
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        SubLevelObserver.super.onSubLevelRemoved(subLevel, reason);
        System.out.println("removed" + reason);
    }

}
