package com.github.krlohnes.expirationcache;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * Interface for a cache with per entry expiration times
 */
public interface ExpiringCache<K, V> {
    void put(K key, V value);

    void put(K key, V value, long expireIn, TimeUnit duration);

    Optional<V> get(K key);

    void delete(K key);

    Integer size();
}
