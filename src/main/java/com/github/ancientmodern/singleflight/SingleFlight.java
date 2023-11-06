package com.github.ancientmodern.singleflight;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class SingleFlight<V> {

    private final ReentrantLock mutex = new ReentrantLock(true);
    private Map<String, Call<V>> keyToCallMap;

    public V exec(final String key, final Callable<V> callable) throws Exception {
        Call<V> call;

        mutex.lock();
        try {
            if (keyToCallMap == null) {
                keyToCallMap = new HashMap<>();
            }

            call = keyToCallMap.get(key);
            if (call != null) {
                mutex.unlock();
                call.countDownLatch.await();

                return call.value;
            }

            call = new Call<V>();
            call.countDownLatch = new CountDownLatch(1);
            keyToCallMap.put(key, call);
        } finally {
            if (mutex.isHeldByCurrentThread()) {
                mutex.unlock();
            }
        }

        try {
            call.value = callable.call();
        } finally {
            mutex.lock();
            try {
                call.countDownLatch.countDown();
                if (keyToCallMap.get(key) == call) {
                    keyToCallMap.remove(key);
                }
            } finally {
                mutex.unlock();
            }
        }

        return call.value;
    }

    public void forget(final String key) {
        mutex.lock();
        try {
            keyToCallMap.remove(key);
        } finally {
            mutex.unlock();
        }
    }

    private static class Call<V> {

        private CountDownLatch countDownLatch;
        private V value;
    }
}
