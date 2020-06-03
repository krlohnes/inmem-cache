package com.github.krlohnes.expirationcache;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class PerformanceTests {
    @State(value = Scope.Benchmark)
    public static class TestState {
        ConcurrentHashMapExpiringCache<Integer, Integer> cache;

        public TestState() {
        }

        @Setup(Level.Trial)
        public void createCache() {
            cache = new ConcurrentHashMapExpiringCache<>();
            IntStream.range(0, 10_000_000).parallel().forEach(i -> cache.put(i, i));
        }

        @TearDown(Level.Trial)
        public void tearDownCache() {
            cache.shutdown();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkGet(TestState state) {
        state.cache.get(ThreadLocalRandom.current().nextInt(0, 10_000_000 + 1));
    }

}
