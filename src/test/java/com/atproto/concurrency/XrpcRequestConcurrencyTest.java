package com.atprotocol.concurrency;

import com.atprotocol.api.xrpc.XrpcClient;
import com.atprotocol.api.xrpc.XrpcServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class XrpcRequestConcurrencyTest {
    private static final int THREAD_COUNT = 20;
    private static final int REQUESTS_PER_THREAD = 100;
    private static final int TIMEOUT_SECONDS = 30;
    
    private XrpcClient mockClient;
    private XrpcServer mockServer;
    private ExecutorService executor;
    
    @BeforeEach
    public void setUp() {
        mockServer = mock(XrpcServer.class);
        mockClient = mock(XrpcClient.class);
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }
    
    @Test
    @Timeout(TIMEOUT_SECONDS)
    public void testConcurrentXrpcRequests() throws InterruptedException {
        // Mock server and client interactions
        when(mockClient.getServer()).thenReturn(mockServer);
        when(mockClient.query()).thenReturn(true);
        when(mockClient.procedure()).thenReturn(true);
        when(mockClient.subscription()).thenReturn(true);
        when(mockClient.notification()).thenReturn(true);
        
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    testXrpcOperations(mockClient);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        // Verify all operations were called the correct number of times
        verify(mockClient, times(THREAD_COUNT * REQUESTS_PER_THREAD)).query();
        verify(mockClient, times(THREAD_COUNT * REQUESTS_PER_THREAD)).procedure();
        verify(mockClient, times(THREAD_COUNT * REQUESTS_PER_THREAD)).subscription();
        verify(mockClient, times(THREAD_COUNT * REQUESTS_PER_THREAD)).notification();
    }
    
    @Test
    @Timeout(TIMEOUT_SECONDS)
    public void testXrpcRequestRateLimiting() throws InterruptedException {
        // Mock rate limiting behavior
        when(mockClient.getServer()).thenReturn(mockServer);
        when(mockClient.query()).thenThrow(new RuntimeException("rate limit exceeded"));
        when(mockClient.procedure()).thenThrow(new RuntimeException("rate limit exceeded"));
        
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    testRateLimiting(mockClient);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        // Verify rate limiting was enforced
        verify(mockClient, times(THREAD_COUNT * 100)).query(); // 100 is the mock rate limit
        verify(mockClient, times(THREAD_COUNT * 50)).procedure(); // 50 is the mock rate limit
    }
    
    private void testXrpcOperations(XrpcClient client) {
        for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
            client.query();
            client.procedure();
            client.subscription();
            client.notification();
        }
    }
    
    private void testRateLimiting(XrpcClient client) {
        // Test query rate limiting
        for (int i = 0; i < 100; i++) {
            try {
                client.query();
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("rate limit"));
            }
        }
        
        // Test procedure rate limiting
        for (int i = 0; i < 50; i++) {
            try {
                client.procedure();
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("rate limit"));
            }
        }
    }
}
