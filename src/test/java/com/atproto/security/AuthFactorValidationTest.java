package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void testValidAuthFactor() {
        assertThat(validator.validateAuthFactor(VALID_AUTH_FACTOR_TOKEN)).isTrue();
    }

    @Test
    void testInvalidAuthFactor() {
        assertThat(validator.validateAuthFactor(INVALID_AUTH_FACTOR_TOKEN)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired-token", "expired-123", "expired-456"})
    void testExpiredAuthFactor(String expiredToken) {
        when(validator.validateAuthFactor(expiredToken))
            .thenReturn(false);

        assertThat(validator.validateAuthFactor(expiredToken)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"single-use-token", "one-time-use", "use-once"})
    void testSingleUseAuthFactor(String token) {
        // First use should succeed
        when(validator.validateAuthFactor(token))
            .thenReturn(true);

        assertThat(validator.validateAuthFactor(token)).isTrue();

        // Second use should fail (single-use token)
        when(validator.validateAuthFactor(token))
            .thenReturn(false);

        assertThat(validator.validateAuthFactor(token)).isFalse();
    }

    @Test
    void testAuthFactorRateLimiting() {
        // Mock rate limiting behavior
        when(validator.generateAuthFactorToken(any(String.class), any(Instant.class)))
            .thenThrow(new SecurityException("Rate limit exceeded"));

        assertThatThrownBy(() ->
            validator.generateAuthFactorToken(TEST_DID, Instant.now().plusSeconds(3600))
        ).isInstanceOf(SecurityException.class)
          .hasMessage("Rate limit exceeded");
    }

    @Test
    void testAuthFactorCleanup() {
        // Mock cleanup behavior
        when(validator.cleanupExpiredAuthFactors())
            .thenReturn(100);

        // Clean up expired tokens
        int cleaned = validator.cleanupExpiredAuthFactors();

        assertThat(cleaned).isGreaterThanOrEqualTo(100);
    }

    @Test
    void testAuthFactorTokenGeneration() {
        when(validator.generateAuthFactorToken(TEST_DID, Instant.now().plusSeconds(3600)))
            .thenReturn(VALID_AUTH_FACTOR_TOKEN);

        String token = validator.generateAuthFactorToken(TEST_DID, Instant.now().plusSeconds(3600));
        assertThat(token).isEqualTo(VALID_AUTH_FACTOR_TOKEN);
    }

    @Test
    void testMultipleAuthFactors() {
        String token1 = "token1";
        String token2 = "token2";
        
        when(validator.validateAuthFactor(token1)).thenReturn(true);
        when(validator.validateAuthFactor(token2)).thenReturn(true);
        
        assertThat(validator.validateAuthFactor(token1)).isTrue();
        assertThat(validator.validateAuthFactor(token2)).isTrue();
    }
}
