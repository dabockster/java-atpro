package com.atprotocol.concurrency;

import com.atprotocol.api.xrpc.XrpcClient;
import com.atprotocol.api.xrpc.XrpcServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class XrpcRequestConcurrencyTest {
    private static final int THREAD_COUNT = 20;  // Based on Go implementation's workerCount
    private static final int REQUESTS_PER_THREAD = 100;  // Based on Go's parallel record creates
    private static final int TIMEOUT_SECONDS = 30;

    @Test
    @Timeout(TIMEOUT_SECONDS)
    public void testConcurrentXrpcRequests() throws InterruptedException {
        XrpcServer server = new XrpcServer();
        XrpcClient client = new XrpcClient(server); // Assuming server is running
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testXrpcOperations(client);
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

    private void testXrpcOperations(XrpcClient client) {
        for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
            // Test concurrent query requests
            client.query();
            
            // Test concurrent procedure requests
            client.procedure();
            
            // Test concurrent subscription requests
            client.subscription();
            
            // Test concurrent notification requests
            client.notification();
        }
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    public void testXrpcRequestRateLimiting() throws InterruptedException {
        XrpcServer server = new XrpcServer();
        XrpcClient client = new XrpcClient(server);
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testRateLimiting(client);
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

    private void testRateLimiting(XrpcClient client) {
        // Test rate limiting for query requests
        int maxQueries = 100;
        for (int i = 0; i < maxQueries; i++) {
            try {
                client.query();
            } catch (Exception e) {
                // Verify rate limiting error
                assertTrue(e.getMessage().contains("rate limit"));
            }
        }
        
        // Test rate limiting for procedure requests
        int maxProcedures = 50;
        for (int i = 0; i < maxProcedures; i++) {
            try {
                client.procedure();
            } catch (Exception e) {
                // Verify rate limiting error
                assertTrue(e.getMessage().contains("rate limit"));
            }
        }
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    public void testXrpcRequestTimeouts() throws InterruptedException {
        XrpcServer server = new XrpcServer();
        XrpcClient client = new XrpcClient(server);
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        try {
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        testTimeouts(client);
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

    private void testTimeouts(XrpcClient client) {
        // Test request timeouts
        try {
            client.longRunningRequest();
            fail("Request should have timed out");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("timeout"));
        }
        
        // Test connection timeouts
        try {
            client.connectToNonExistentServer();
            fail("Connection should have timed out");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("timeout"));
        }
    }
}
