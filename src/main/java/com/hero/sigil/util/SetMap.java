package com.hero.sigil.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks sets of values grouped by a string key.
 */
public final class SetMap<T> {
    private final Map<String, Set<T>> values = new HashMap<>();

    public int add(String key, T value) {
        Set<T> keyValues = values.computeIfAbsent(key, ignored -> new HashSet<>());
        keyValues.add(value);
        return keyValues.size();
    }

    public Set<T> get(String key) {
        return values.get(key);
    }

    public int size(String key) {
        Set<T> keyValues = values.get(key);
        return keyValues == null ? 0 : keyValues.size();
    }

    public void remove(String key) {
        values.remove(key);
    }
}
