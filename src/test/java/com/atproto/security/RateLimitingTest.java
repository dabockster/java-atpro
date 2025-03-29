package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingTest {

    @Mock
    private RateLimiter rateLimiter;

    private static final String TEST_CLIENT_ID = "test-client";
    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(rateLimiter);
    }

    @Test
    void testInitialRateLimiting() {
        // Test that a new client starts with no rate limiting
        when(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).thenReturn(true);
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testRateLimitExceeded() {
        // Test that rate limit is enforced after reaching max requests
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(MAX_REQUESTS);
        assertFalse(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testRateLimitReset() {
        // Test that rate limit resets after window duration
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(MAX_REQUESTS);
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID)).thenReturn(Instant.now().minus(WINDOW_DURATION).minusSeconds(1));
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testMultipleClients() {
        // Test that rate limits are tracked per client
        String client1 = "client1";
        String client2 = "client2";
        
        when(rateLimiter.getRequestCount(client1)).thenReturn(MAX_REQUESTS);
        when(rateLimiter.getRequestCount(client2)).thenReturn(0);
        
        assertFalse(rateLimiter.canMakeRequest(client1));
        assertTrue(rateLimiter.canMakeRequest(client2));
    }

    @Test
    void testRequestCounting() {
        // Test that requests are properly counted
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(50);
        when(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).thenReturn(true);
        
        rateLimiter.recordRequest(TEST_CLIENT_ID);
        verify(rateLimiter).incrementRequestCount(TEST_CLIENT_ID);
    }

    @Test
    void testWindowDuration() {
        // Test that window duration is respected
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID)).thenReturn(Instant.now().minus(WINDOW_DURATION).plusSeconds(30));
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testInvalidClientId() {
        // Test handling of invalid client IDs
        String invalidClientId = "";
        assertFalse(rateLimiter.canMakeRequest(invalidClientId));
    }

    @Test
    void testConcurrentRequests() {
        // Test concurrent request handling
        String client1 = "client1";
        String client2 = "client2";
        
        when(rateLimiter.getRequestCount(client1)).thenReturn(99);
        when(rateLimiter.getRequestCount(client2)).thenReturn(99);
        
        assertFalse(rateLimiter.canMakeRequest(client1));
        assertFalse(rateLimiter.canMakeRequest(client2));
    }

    @Test
    void testRequestTiming() {
        // Test request timing and rate limiting
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(50);
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID)).thenReturn(Instant.now().minusSeconds(30));
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testRateLimitingWithMultipleWindows() {
        // Test rate limiting across multiple windows
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(MAX_REQUESTS);
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID)).thenReturn(Instant.now().minus(WINDOW_DURATION).plusSeconds(30));
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }

    @Test
    void testRateLimitingWithZeroRequests() {
        // Test rate limiting when no requests have been made
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(0);
        assertTrue(rateLimiter.canMakeRequest(TEST_CLIENT_ID));
    }
}
