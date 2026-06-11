package com.hollingsworth.ars_sable.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// Tracks a 1 BlockPos to many relations, used for when a blockpos enters and exits sublevels
public class TrackedPosIndex<P, K> {
    private final Map<P, HashSet<K>> index = new HashMap<>();

    public void add(P pos, K key) {
        index.computeIfAbsent(pos, ignored -> new HashSet<>()).add(key);
    }

    public void remove(P pos, K key) {
        HashSet<K> keys = index.get(pos);
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            index.remove(pos);
        }
    }

    public void clear() {
        index.clear();
    }

    public Set<K> get(P pos) {
        HashSet<K> keys = index.get(pos);
        return keys == null ? Set.of() : keys;
    }

    public Set<K> removeAll(P pos) {
        HashSet<K> keys = index.remove(pos);
        return keys == null ? Set.of() : keys;
    }

    public boolean moveAll(P oldPos, P newPos, Predicate<K> mover) {
        HashSet<K> keys = index.remove(oldPos);
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        boolean moved = false;
        for (K key : keys) {
            if (mover.test(key)) {
                add(newPos, key);
                moved = true;
            } else {
                add(oldPos, key);
            }
        }
        return moved;
    }
}
