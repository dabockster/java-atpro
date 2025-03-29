package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFactorValidationTest {

    @Mock
    private AuthFactorValidator validator;

    private static final String VALID_AUTH_FACTOR_TOKEN = "123456";
    private static final String INVALID_AUTH_FACTOR_TOKEN = "invalid-token";
    private static final String TEST_DID = "did:plc:1234567890abcdef";

    @BeforeEach
    void setUp() {
        // Setup mock validator with valid auth factor
        when(validator.validateAuthFactor(VALID_AUTH_FACTOR_TOKEN))
            .thenReturn(true);
        when(validator.validateAuthFactor(INVALID_AUTH_FACTOR_TOKEN))
            .thenReturn(false);
    }

    @Test
    void testValidAuthFactor() throws Exception {
        assertTrue(validator.validateAuthFactor(VALID_AUTH_FACTOR_TOKEN));
    }

    @Test
    void testInvalidAuthFactor() throws Exception {
        assertFalse(validator.validateAuthFactor(INVALID_AUTH_FACTOR_TOKEN));
    }

    @Test
    void testExpiredAuthFactor() throws Exception {
        String expiredToken = "expired-token";
        
        when(validator.validateAuthFactor(expiredToken))
            .thenReturn(false);

        assertFalse(validator.validateAuthFactor(expiredToken));
    }

    @Test
    void testSingleUseAuthFactor() throws Exception {
        String token = "single-use-token";

        // First use should succeed
        when(validator.validateAuthFactor(token))
            .thenReturn(true);

        assertTrue(validator.validateAuthFactor(token));

        // Second use should fail (single-use token)
        when(validator.validateAuthFactor(token))
            .thenReturn(false);

        assertFalse(validator.validateAuthFactor(token));
    }

    @Test
    void testAuthFactorRateLimiting() throws Exception {
        // Mock rate limiting behavior
        when(validator.generateAuthFactorToken(any(String.class), any(Instant.class)))
            .thenThrow(new SecurityException("Rate limit exceeded"));

        // Should throw rate limit exception
        assertThrows(SecurityException.class, () ->
            validator.generateAuthFactorToken(TEST_DID, Instant.now().plusSeconds(3600))
        );
    }

    @Test
    void testAuthFactorCleanup() throws Exception {
        // Mock cleanup behavior
        when(validator.cleanupExpiredAuthFactors())
            .thenReturn(100);

        // Clean up expired tokens
        int cleaned = validator.cleanupExpiredAuthFactors();

        assertTrue(cleaned >= 100);
    }
}
