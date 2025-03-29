package com.atproto.concurrency;

import com.atproto.events.EventManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class EventStreamingLoadTest {
    private static final int THREAD_COUNT = 20;  // Based on Go implementation's workerCount
    private static final int SUBSCRIPTIONS_PER_THREAD = 5;  // Reduced from 10 to match Go's parallel operations
    private static final int EVENTS_PER_SUBSCRIPTION = 100;  // Based on Go's backfill parallel record creates

    @Test
    @Timeout(60)
    public void testEventStreamingUnderLoad() throws InterruptedException {
        EventManager eventManager = new EventManager();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testEventStreaming(eventManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

    private void testEventStreaming(EventManager eventManager) {
        // Create multiple subscriptions
        for (int i = 0; i < SUBSCRIPTIONS_PER_THREAD; i++) {
            String subscriptionId = eventManager.createSubscription();
            
            // Test concurrent event publishing
            for (int j = 0; j < EVENTS_PER_SUBSCRIPTION; j++) {
                eventManager.publishEvent(subscriptionId);
            }
            
            // Test concurrent event consumption
            eventManager.consumeEvents(subscriptionId);
            
            // Test concurrent subscription management
            eventManager.updateSubscription(subscriptionId);
            eventManager.deleteSubscription(subscriptionId);
        }
    }

    @Test
    @Timeout(60)
    public void testEventBatchProcessing() throws InterruptedException {
        EventManager eventManager = new EventManager();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testBatchProcessing(eventManager);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

    private void testBatchProcessing(EventManager eventManager) {
        // Create a subscription
        String subscriptionId = eventManager.createSubscription();
        
        // Test large batch processing
        int batchSize = 1000;
        eventManager.publishBatch(subscriptionId, batchSize);
        
        // Test concurrent batch consumption
        eventManager.consumeBatch(subscriptionId, batchSize);
        
        // Test batch cleanup
        eventManager.cleanupBatch(subscriptionId);
    }
}
