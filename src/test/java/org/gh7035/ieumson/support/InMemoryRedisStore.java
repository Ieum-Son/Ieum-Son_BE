package org.gh7035.ieumson.support;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryRedisStore {

    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expireAtMillis = new ConcurrentHashMap<>();

    public synchronized String get(String key) {
        evictIfExpired(key);
        return values.get(key);
    }

    public synchronized void set(String key, String value, Duration ttl) {
        values.put(key, value);
        if (ttl == null) {
            expireAtMillis.remove(key);
            return;
        }
        expireAtMillis.put(key, System.currentTimeMillis() + ttl.toMillis());
    }

    public synchronized boolean setIfAbsent(String key, String value, Duration ttl) {
        if (get(key) != null) {
            return false;
        }
        set(key, value, ttl);
        return true;
    }

    public synchronized long increment(String key) {
        evictIfExpired(key);
        long next = 1;
        String current = values.get(key);
        if (current != null) {
            next = Long.parseLong(current) + 1;
        }
        values.put(key, Long.toString(next));
        return next;
    }

    public synchronized String getAndDelete(String key) {
        String value = get(key);
        delete(key);
        return value;
    }

    public synchronized boolean delete(String key) {
        expireAtMillis.remove(key);
        return values.remove(key) != null;
    }

    public synchronized boolean expire(String key, long timeout, TimeUnit unit) {
        evictIfExpired(key);
        if (!values.containsKey(key)) {
            return false;
        }
        expireAtMillis.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
        return true;
    }

    public synchronized Long getExpire(String key, TimeUnit unit) {
        if (!values.containsKey(key)) {
            return -2L;
        }
        Long deadline = expireAtMillis.get(key);
        if (deadline == null) {
            return -1L;
        }
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
            delete(key);
            return -2L;
        }
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    public synchronized void clear() {
        values.clear();
        expireAtMillis.clear();
    }

    private void evictIfExpired(String key) {
        Long deadline = expireAtMillis.get(key);
        if (deadline != null && deadline <= System.currentTimeMillis()) {
            expireAtMillis.remove(key);
            values.remove(key);
        }
    }
}
