package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private RateLimiter rateLimiter;

    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimiter, "maxRequests", MAX_REQUESTS);
        ReflectionTestUtils.setField(rateLimiter, "windowDuration", WINDOW_DURATION);
    }

    @Test
    void testRateLimitingWithinLimit() throws Exception {
        // Should allow requests within limit
        for (int i = 0; i < MAX_REQUESTS - 1; i++) {
            assertTrue(rateLimiter.checkRateLimit(request));
        }
    }

    @Test
    void testRateLimitingExceeded() throws Exception {
        // Exceed the limit
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.checkRateLimit(request);
        }

        // Next request should be blocked
        assertThrows(SecurityException.class, () ->
            rateLimiter.checkRateLimit(request)
        );
    }

    @Test
    void testRateLimitingReset() throws Exception {
        // Exceed the limit
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.checkRateLimit(request);
        }

        // Wait for window to reset
        Thread.sleep(WINDOW_DURATION.toMillis() + 100);

        // Should be allowed again
        assertTrue(rateLimiter.checkRateLimit(request));
    }

    @Test
    void testDifferentClientRateLimiting() throws Exception {
        // Create two different requests (simulating different clients)
        XrpcRequest request1 = mock(XrpcRequest.class);
        XrpcRequest request2 = mock(XrpcRequest.class);

        // Both should be allowed within their own limits
        for (int i = 0; i < MAX_REQUESTS - 1; i++) {
            assertTrue(rateLimiter.checkRateLimit(request1));
            assertTrue(rateLimiter.checkRateLimit(request2));
        }
    }

    @Test
    void testRateLimitingHeaders() throws Exception {
        XrpcResponse response = rateLimiter.applyRateLimitHeaders(new XrpcResponse(), request);
        
        assertNotNull(response.getHeaders().get("X-RateLimit-Limit"));
        assertNotNull(response.getHeaders().get("X-RateLimit-Remaining"));
        assertNotNull(response.getHeaders().get("X-RateLimit-Reset"));
    }
}
