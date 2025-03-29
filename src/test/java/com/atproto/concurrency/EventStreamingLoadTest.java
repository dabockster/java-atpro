package com.atproto.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.Assertions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventStreamingLoadTest {
    private static final int THREAD_COUNT = 20;
    private static final int SUBSCRIPTIONS_PER_THREAD = 5;
    private static final int EVENTS_PER_SUBSCRIPTION = 100;
    
    private EventManager eventManager;
    
    @BeforeEach
    public void setup() {
        eventManager = mock(EventManager.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 30})
    @Timeout(60)
    public void testEventStreamingUnderLoad(int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
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
            
            // Verify using AssertJ for better assertions
            Assertions.assertThat(eventManager)
                .isNotNull()
                .hasFieldOrProperty("eventQueue");
            
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

    @ParameterizedTest
    @ValueSource(ints = {1000, 5000, 10000})
    @Timeout(60)
    public void testEventBatchProcessing(int batchSize) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testBatchProcessing(eventManager, batchSize);
                        verifyBatchProcessingOperations(eventManager, batchSize);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            // Verify using AssertJ for better assertions
            Assertions.assertThat(eventManager)
                .isNotNull()
                .hasFieldOrProperty("batchProcessor");
            
        } finally {
            executor.shutdown();
        }
    }

    private void testBatchProcessing(EventManager eventManager, int batchSize) {
        String subscriptionId = "test-subscription";
        
        // Test large batch processing
        eventManager.publishBatch(subscriptionId, batchSize);
        
        // Test concurrent batch consumption
        eventManager.consumeBatch(subscriptionId, batchSize);
        
        // Test batch cleanup
        eventManager.cleanupBatch(subscriptionId);
    }

    private void verifyBatchProcessingOperations(EventManager eventManager, int batchSize) {
        verify(eventManager).createSubscription();
        verify(eventManager).publishBatch(any(String.class), eq(batchSize));
        verify(eventManager).consumeBatch(any(String.class), eq(batchSize));
        verify(eventManager).cleanupBatch(any(String.class));
    }
}
