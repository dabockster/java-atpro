package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFactorValidationTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private AuthFactorValidator validator;

    private static final String VALID_AUTH_FACTOR_TOKEN = "123456";
    private static final String INVALID_AUTH_FACTOR_TOKEN = "invalid-token";
    private static final String TEST_DID = "did:plc:1234567890abcdef";

    @BeforeEach
    void setUp() {
        // Setup mock request with valid auth factor
        when(request.getHeader("X-Auth-Factor-Token"))
            .thenReturn(VALID_AUTH_FACTOR_TOKEN);
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
        String expiredToken = validator.generateAuthFactorToken(
            TEST_DID,
            Instant.now().minusSeconds(1)
        );

        assertFalse(validator.validateAuthFactor(expiredToken));
    }

    @Test
    void testSingleUseAuthFactor() throws Exception {
        String token = validator.generateAuthFactorToken(
            TEST_DID,
            Instant.now().plusSeconds(3600)
        );

        // First use should succeed
        assertTrue(validator.validateAuthFactor(token));

        // Second use should fail (single-use token)
        assertFalse(validator.validateAuthFactor(token));
    }

    @Test
    void testAuthFactorRateLimiting() throws Exception {
        // Generate multiple tokens quickly
        for (int i = 0; i < 100; i++) {
            validator.generateAuthFactorToken(
                TEST_DID,
                Instant.now().plusSeconds(3600)
            );
        }

        // Should throw rate limit exception
        assertThrows(SecurityException.class, () ->
            validator.generateAuthFactorToken(
                TEST_DID,
                Instant.now().plusSeconds(3600)
            )
        );
    }

    @Test
    void testAuthFactorCleanup() throws Exception {
        // Generate expired tokens
        for (int i = 0; i < 100; i++) {
            validator.generateAuthFactorToken(
                TEST_DID,
                Instant.now().minusSeconds(3600)
            );
        }

        // Clean up expired tokens
        int cleaned = validator.cleanupExpiredAuthFactors();

        assertTrue(cleaned >= 100);
    }
}
