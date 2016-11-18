package com.godaddy.vps4.util;

import java.util.Map;
import java.util.concurrent.*;

public class DefaultCache<K, V> {
    private final Map<K, V> cache;

    public DefaultCache() {
        this.cache = new ConcurrentHashMap<K, V>();
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }
}
