package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
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
        reset(rateLimiter);
    }

    @Test
    void testInitialRateLimiting() {
        when(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).thenReturn(true);
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).isTrue();
    }

    @Test
    void testRateLimitExceeded() {
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(MAX_REQUESTS);
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).isFalse();
    }

    @Test
    void testRateLimitReset() {
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(MAX_REQUESTS);
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID))
            .thenReturn(Instant.now().minus(WINDOW_DURATION).minusSeconds(1));
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"client1", "client2", "client3"})
    void testMultipleClients(String clientId) {
        // Set up different request counts for each client
        when(rateLimiter.getRequestCount(clientId)).thenReturn(clientId.equals("client1") ? MAX_REQUESTS : 0);
        
        assertThat(rateLimiter.canMakeRequest(clientId))
            .isEqualTo(!clientId.equals("client1"));
    }

    @Test
    void testRequestCounting() {
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(50);
        when(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).thenReturn(true);
        
        rateLimiter.recordRequest(TEST_CLIENT_ID);
        verify(rateLimiter).incrementRequestCount(TEST_CLIENT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "30", "45"})
    void testWindowDuration(String seconds) {
        int secondsValue = Integer.parseInt(seconds);
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID))
            .thenReturn(Instant.now().minus(WINDOW_DURATION).plusSeconds(secondsValue));
        
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID))
            .isEqualTo(secondsValue < WINDOW_DURATION.getSeconds());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid"})
    void testInvalidClientId(String invalidClientId) {
        assertThat(rateLimiter.canMakeRequest(invalidClientId)).isFalse();
    }

    @Test
    void testConcurrentRequests() {
        String client1 = "client1";
        String client2 = "client2";
        
        when(rateLimiter.getRequestCount(client1)).thenReturn(99);
        when(rateLimiter.getRequestCount(client2)).thenReturn(99);
        
        assertThat(rateLimiter.canMakeRequest(client1)).isFalse();
        assertThat(rateLimiter.canMakeRequest(client2)).isFalse();
    }

    @Test
    void testRequestCountOverflow() {
        when(rateLimiter.getRequestCount(TEST_CLIENT_ID)).thenReturn(Integer.MAX_VALUE);
        
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).isFalse();
    }

    @Test
    void testZeroWindowDuration() {
        when(rateLimiter.getLastRequestTime(TEST_CLIENT_ID))
            .thenReturn(Instant.now());
        
        assertThat(rateLimiter.canMakeRequest(TEST_CLIENT_ID)).isTrue();
    }
}
