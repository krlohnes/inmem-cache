package com.github.krlohnes.expirationcache;

import java.util.concurrent.*;
import java.util.Optional;

public class ConcurrentHashMapExpiringCache<K, V> implements ExpiringCache<K, V> {

    private static final String NULL_EXCEPTION = "Neither key nor value allowed to be null";

    private final ConcurrentMap<K, ExpiringCacheEntry<K, V>> map;

    private final BlockingQueue<ExpiringCacheEntry<K, V>> delayQueue;

    final ScheduledExecutorService executorService;

    public ConcurrentHashMapExpiringCache() {
        delayQueue = new DelayQueue<>();
        //Would be better to provide initial capacity to avoid resizing early
        //as it is generally an expensive operation and a load factor (density of
        //cache) to help with this.
        map = new ConcurrentHashMap<>();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::clean, 0L, 1L, TimeUnit.SECONDS);
    }

    public ConcurrentHashMapExpiringCache(int initialCapacity) {
        delayQueue = new DelayQueue<>();
        map = new ConcurrentHashMap<>(initialCapacity);
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::clean, 0L, 1L, TimeUnit.SECONDS);
    }

    public ConcurrentHashMapExpiringCache(int initialCapacity, float loadFactor) {
        delayQueue = new DelayQueue<>();
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::clean, 0L, 1L, TimeUnit.SECONDS);
    }

    public ConcurrentHashMapExpiringCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        delayQueue = new DelayQueue<>();
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::clean, 0L, 1L, TimeUnit.SECONDS);
    }

    //For testing
    protected ConcurrentHashMapExpiringCache(ConcurrentMap<K, ExpiringCacheEntry<K, V>> map) {
        delayQueue = new DelayQueue();
        this.map = map;
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void put(K key, V value) {
        nullCheck(key, value);
        map.put(key, new ExpiringCacheEntry<>(key, value));
    }

    @Override
    public void put(K key, V value, long expireIn, TimeUnit duration) {
        nullCheck(key, value);
        Long expirationTime = duration.convert(expireIn, TimeUnit.MILLISECONDS) + System.currentTimeMillis();
        map.compute(key, (k, v) -> {
            try {
                //DelayQueue is unbounded so put never blocks
                delayQueue.put(new ExpiringCacheEntry<>(key, null, expirationTime));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return new ExpiringCacheEntry<>(null, value, expirationTime);
        });
    }

    @Override
    public Optional<V> get(K key) {
        if (key == null) {
            return Optional.empty();
        } else {
            long currentTime = System.currentTimeMillis();
            ExpiringCacheEntry<K, V> entry = map.get(key);
            if (entry != null &&
                    (entry.getExpirationTime() == ExpiringCacheEntry.DO_NOT_EXPIRE ||
                    currentTime <= entry.getExpirationTime())) {
                return Optional.of(entry.getValue());
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public Integer size() {
        return map.size();
    }

    @Override
    public void delete(K key) {
        if (key == null) {
            throw new IllegalArgumentException(NULL_EXCEPTION);
        }
        map.remove(key);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private void nullCheck(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException(NULL_EXCEPTION);
        }
    }

    private void clean() {
        //This can probably get backed up on a large enough N value with
        //expirations which are all in roughly the same time frame;
        ExpiringCacheEntry<K, V> expiringCacheEntry = delayQueue.poll();
        while (expiringCacheEntry != null) {
            final Long entryExpirationTime = expiringCacheEntry.getExpirationTime();
            map.compute(expiringCacheEntry.getKey(), (k, v) -> {
                if (v == null || v.getExpirationTime().equals(entryExpirationTime)) {
                    return null;
                } else {
                    return v;
                }
            });
            expiringCacheEntry = delayQueue.poll();
        }
    }
}




