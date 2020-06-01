package com.github.krlohnes.expirationcache;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.Assert;

public class TestExpiringCacheEntry {

    @Test
    public void testNonExpiringConstructor() {
        ExpiringCacheEntry<String, String> entry = new ExpiringCacheEntry<>("foo", "bar");
        Assert.assertEquals(ExpiringCacheEntry.DO_NOT_EXPIRE, entry.getExpirationTime());
    }

    @Test
    public void testExpiringConstructor() {
        ExpiringCacheEntry<String, String> entry = new ExpiringCacheEntry<>("foo", "bar", 1000L);
        Assert.assertEquals(Long.valueOf(1000L), entry.getExpirationTime());
    }

    @Test
    public void getDelay() {
        ExpiringCacheEntry<String, String> entry = new ExpiringCacheEntry<>("foo", "bar", System.currentTimeMillis() + 10000L);
        Long delay = entry.getDelay(TimeUnit.MILLISECONDS);
        Assert.assertEquals(Long.valueOf(10000L), delay);
    }

}

