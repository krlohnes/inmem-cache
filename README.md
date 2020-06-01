# Exercise for an interview

This is an in memory key value cache with optional cache expiration time.

The implemenation given here is backed by a
`java.util.concurrent.ConcurrentHashMap` for optimal performance and ease of
implementation.

For performance considerations of a ConcurrentHashMap see
[the java docs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html)


This implementation uses `ConcurrentHashMap.compute` for it's put operations as
this is computed atomically and allows us to add an expiration entry to a
`DelayQueue`

The ease of implementation using a `DelayQueue` is ideal under the time constraints
of the project. It does introduce some complications. The `DelayQueue` will
not remove duplicate key entries, so removal due to expiration must assert that
the expiration time is identical on both values. This also means that the delay
queue will store a key and a timestamp for _every_ put operation with a timeout,
increasing the memory overhead of this cache.

The implementation is also a little less than ideal due to its heavy use of
`System.currentTimeMillis()` which, depending on the platform on which the
program is running, can be a little slow. A more ideal solution might be to
use something like a timing wheel to avoid additional calls to
`System.currentTimeMillis()`. See this paper on timing wheels
[Timing wheels](http://www.cs.columbia.edu/~nahum/w6998/papers/ton97-timing-wheels.pdf)
or a nice implementation of a timing wheel for events in a cache in [Ben Manes'
caffeine implementation](https://github.com/ben-manes/caffeine/blob/master/caffeine/src/main/java/com/github/benmanes/caffeine/cache/TimerWheel.java)


The implementation as it sits also has a bottleneck around a single thread
executing the removal tick every second through the use of a scheduled
executor service which should be shut down on program termination. If there
were a burst of keys up to the maximum specified all being put to expire
at about the same time, this thread would back up quite a bit. While this is
somewhat helped by not allowing gets on expired values in the `get` method
logic, adding more threads for additional concurrency on demand as the size of
the cache increases might be able to help with this.


This cache has unit tests that can be run with `./gradlew test`

This cache also has a single performance written using
[JMH](https://openjdk.java.net/projects/code-tools/jmh/)

This can be run by using `./gradlew jmh`

Additional options for running the JMH tests can be found at the
[jmh gradle plugin page](https://github.com/melix/jmh-gradle-plugin).