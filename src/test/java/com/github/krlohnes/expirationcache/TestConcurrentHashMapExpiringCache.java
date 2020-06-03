package com.github.krlohnes.expirationcache;

import java.util.Map;
import static org.mockito.Mockito.*;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Assert;

public class TestConcurrentHashMapExpiringCache {

    private ConcurrentHashMapExpiringCache<Integer, Integer> map = new ConcurrentHashMapExpiringCache<>();
    private Random random = new Random();

    private void testConcurrentOperation(
            BiConsumer<Integer, Integer> methodToTest,
            ExpiringCache<Integer, Integer> map,
            Integer iterations,
            Integer finalMapSize)
            throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            List<Thread> listOfThreads = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        latch.await();
                        for (int j = 0; j < iterations; j++) {
                            methodToTest.accept(j, j);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                listOfThreads.add(thread);
                thread.start();
            }
            latch.countDown();
            for (Thread thread : listOfThreads) {
                thread.join();
            }
            Assert.assertEquals(finalMapSize, finalMapSize);
            }

    @Test
    public void putAndRemove_threadSafety() throws InterruptedException {
        ConcurrentHashMapExpiringCache<Integer, Integer> localmap = new ConcurrentHashMapExpiringCache<>();
        testConcurrentOperation((k, v) -> {
            localmap.put(k, v);
        }, localmap, 1000, 1000);
        testConcurrentOperation((k, v) -> {
            localmap.delete(k);
        }, localmap, 1000, 0);
    }

    @Test
    public void putWithDuration_threadSafety() throws InterruptedException {
        ConcurrentHashMapExpiringCache<Integer, Integer> localmap = new ConcurrentHashMapExpiringCache<>();
        testConcurrentOperation((k, v) -> {
            localmap.put(k, v, 10, TimeUnit.SECONDS);
        }, localmap, 1000, 1000);
    }

    @Test
    public void put() {
        Integer key = random.nextInt();
        map.put(key, -123);
        Assert.assertEquals(Long.valueOf(-123), Long.valueOf(map.get(key).get()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void put_whenKeyIsNull() {
        map.put(null, -123);
        Assert.fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void put_whenValueIsNull() {
        map.put(-123, null);
        Assert.fail();
    }

    @Test
    public void putWithDuration() throws InterruptedException {
        map.put(-321, -321, 1, TimeUnit.SECONDS);
        Assert.assertEquals(Long.valueOf(-321), Long.valueOf(map.get(-321).get()));
        Thread.sleep(3000L);
        Assert.assertFalse(map.get(-321).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void putWithDuration_whenKeyIsNull() {
        map.put(null, -321, 1, TimeUnit.SECONDS);
        Assert.fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void putWithDuration_whenValueIsNull() {
        map.put(-321, null, 1, TimeUnit.SECONDS);
        Assert.fail();
    }

    @Test
    public void get() {
        map.put(-617, -617);
        Assert.assertEquals(Long.valueOf(-617), Long.valueOf(map.get(-617).get()));
    }

    @Test
    public void get_whenKeyIsNull() {
        Assert.assertFalse(map.get(null).isPresent());
    }

    @Test
    public void testDelete() {
        map.put(-781, -781);
        map.delete(-781);
        Assert.assertFalse(map.get(-781).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDelete_whenKeyIsNull() {
        map.delete(null);
    }

    @Test
    public void testGet_timeoutCondition_pastExpirationTime() {
        ConcurrentMap<Integer, ExpiringCacheEntry<Integer, Integer>> mockMap = mock(ConcurrentMap.class);
        ConcurrentHashMapExpiringCache<Integer, Integer> cache = new ConcurrentHashMapExpiringCache<>(mockMap);
        when(mockMap.get(any(Integer.class))).thenReturn(new ExpiringCacheEntry<>(1, 1, 1L));
        Assert.assertFalse(cache.get(1).isPresent());
        cache.shutdown();
    }

    @Test
    public void testGet_timeoutCondition_doNotExpire() {
        ConcurrentMap<Integer, ExpiringCacheEntry<Integer, Integer>> mockMap = mock(ConcurrentMap.class);
        ConcurrentHashMapExpiringCache<Integer, Integer> cache = new ConcurrentHashMapExpiringCache<>(mockMap);
        when(mockMap.get(any(Integer.class))).thenReturn(new ExpiringCacheEntry<>(1, 1, ExpiringCacheEntry.DO_NOT_EXPIRE));
        Assert.assertTrue(cache.get(1).isPresent());
        cache.shutdown();
    }

    @Test
    public void testGet_timeoutCondition_unexpired() {
        ConcurrentMap<Integer, ExpiringCacheEntry<Integer, Integer>> mockMap = mock(ConcurrentMap.class);
        ConcurrentHashMapExpiringCache<Integer, Integer> cache = new ConcurrentHashMapExpiringCache<>(mockMap);
        when(mockMap.get(any(Integer.class))).thenReturn(new ExpiringCacheEntry<>(1, 1, System.currentTimeMillis() + 1000L));
        Assert.assertTrue(cache.get(1).isPresent());
        cache.shutdown();
    }

    @Test
    public void testDelete_withExpiringKey() throws InterruptedException {
        map.put(-413, -413, 1, TimeUnit.SECONDS);
        Assert.assertEquals(Long.valueOf(-413), Long.valueOf(map.get(-413).get()));
        map.delete(-413);
        Assert.assertFalse(map.get(-413).isPresent());
        Thread.sleep(3000L);
        Assert.assertTrue(true);
    }

}
