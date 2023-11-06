package com.github.ancientmodern;

import com.github.ancientmodern.singleflight.SingleFlight;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final SingleFlight<String> singleFlight = new SingleFlight<>();

    public static void main(String[] args) {
        int numTasks = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numTasks);
        CountDownLatch latch = new CountDownLatch(numTasks);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numTasks; i++) {
            int count = i;
            executorService.submit(() -> {
                String data;
                try {
                    data = singleFlight.exec("key", () -> getDataFromDB("key"));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                System.out.printf("%d: %s\n", count, data);
                latch.countDown();
            });
        }

        try {
            latch.await();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Main thread interrupted while waiting for tasks to finish.");
        } finally {
            executorService.shutdown();
        }
    }

    public static String getDataFromDB(String key) {
        System.out.printf("get %s from DB\n", key);
        return "data";
    }
}