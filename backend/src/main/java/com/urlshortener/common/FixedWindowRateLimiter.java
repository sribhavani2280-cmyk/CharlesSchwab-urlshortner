package com.urlshortener.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal fixed-window in-memory rate limiter.
 * Prototype-grade: single-node only. A distributed limiter (e.g. Redis) is a documented scaling step.
 */
public class FixedWindowRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public synchronized boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Window w = windows.get(key);
        if (w == null || now.isAfter(w.resetAt)) {
            windows.put(key, new Window(now.plus(window), 1));
            return true;
        }
        if (w.count < maxRequests) {
            w.count++;
            return true;
        }
        return false;
    }

    private static final class Window {
        private final Instant resetAt;
        private int count;

        private Window(Instant resetAt, int count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
