package com.atproto.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventStreamingLoadTest {
    private static final int THREAD_COUNT = 20;
    private static final int SUBSCRIPTIONS_PER_THREAD = 5;
    private static final int EVENTS_PER_SUBSCRIPTION = 100;
    
    private EventManager eventManager;
    
    @BeforeEach
    public void setup() {
        eventManager = mock(EventManager.class);
    }

    @Test
    @Timeout(60)
    public void testEventStreamingUnderLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testEventStreaming(eventManager);
                        verifyEventStreamingOperations(eventManager);
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
        for (int i = 0; i < SUBSCRIPTIONS_PER_THREAD; i++) {
            String subscriptionId = "subscription-" + i;
            
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

    private void verifyEventStreamingOperations(EventManager eventManager) {
        verify(eventManager, times(SUBSCRIPTIONS_PER_THREAD)).createSubscription();
        verify(eventManager, times(SUBSCRIPTIONS_PER_THREAD * EVENTS_PER_SUBSCRIPTION)).publishEvent(any(String.class));
        verify(eventManager, times(SUBSCRIPTIONS_PER_THREAD)).consumeEvents(any(String.class));
        verify(eventManager, times(SUBSCRIPTIONS_PER_THREAD)).updateSubscription(any(String.class));
        verify(eventManager, times(SUBSCRIPTIONS_PER_THREAD)).deleteSubscription(any(String.class));
    }

    @Test
    @Timeout(60)
    public void testEventBatchProcessing() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testBatchProcessing(eventManager);
                        verifyBatchProcessingOperations(eventManager);
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
        String subscriptionId = "test-subscription";
        int batchSize = 1000;
        
        // Test large batch processing
        eventManager.publishBatch(subscriptionId, batchSize);
        
        // Test concurrent batch consumption
        eventManager.consumeBatch(subscriptionId, batchSize);
        
        // Test batch cleanup
        eventManager.cleanupBatch(subscriptionId);
    }

    private void verifyBatchProcessingOperations(EventManager eventManager) {
        verify(eventManager).createSubscription();
        verify(eventManager).publishBatch(any(String.class), eq(1000));
        verify(eventManager).consumeBatch(any(String.class), eq(1000));
        verify(eventManager).cleanupBatch(any(String.class));
    }
}
