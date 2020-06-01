package com.github.krlohnes.expirationcache;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ExpiringCacheEntry<K, V> implements Delayed {

    public static final Long DO_NOT_EXPIRE = -1L;

    private final K key;
    private final V value;
    private final Long expirationTime;

    public ExpiringCacheEntry(K key, V value, Long expirationTimeInMillis) {
        this.key = key;
        this.value = value;
        this.expirationTime = expirationTimeInMillis;
    }

    public ExpiringCacheEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.expirationTime = DO_NOT_EXPIRE;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expirationTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public V getValue() {
        return value;
    }

    public K getKey() {
        return key;
    }

}


