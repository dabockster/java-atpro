package com.atproto.security;

import com.atproto.security.jwt.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Assertions;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class JWTTokenTest {

    @InjectMocks
    private JWTService jwtService;

    private static final String TEST_AUDIENCE = "test-audience";
    private static final String TEST_SUBJECT = "test-subject";
    private static final String ACCESS_TOKEN_TYPE = "at+jwt";
    private static final String REFRESH_TOKEN_TYPE = "refresh+jwt";
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void testAccessTokenGenerationAndValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusMinutes(30), // Max 30 minutes for access tokens
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        Assertions.assertTrue(jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE));
    }

    @Test
    void testRefreshTokenGenerationAndValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusHours(24), // Max 24 hours for refresh tokens
            keyPair.getPrivate(),
            REFRESH_TOKEN_TYPE
        );

        Assertions.assertTrue(jwtService.validateToken(token, keyPair.getPublic(), REFRESH_TOKEN_TYPE));
    }

    @Test
    void testTokenTypeValidation() throws Exception {
        String accessToken = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusMinutes(30),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        // Should fail if token type doesn't match
        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(accessToken, keyPair.getPublic(), REFRESH_TOKEN_TYPE)
        );
    }

    @Test
    void testTokenLifetimeValidation() throws Exception {
        // Test access token with too long lifetime
        String longLivedToken = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusHours(1), // 1 hour > 30 minutes
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(longLivedToken, keyPair.getPublic(), ACCESS_TOKEN_TYPE)
        );
    }

    @Test
    void testTokenBindingToDPoPKey() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusMinutes(30),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        // Create a different key pair to simulate different DPoP key
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair differentKeyPair = generator.generateKeyPair();

        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, differentKeyPair.getPublic(), ACCESS_TOKEN_TYPE)
        );
    }

    @Test
    void testTokenAudienceValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusMinutes(30),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE, "different-audience")
        );
    }

    @Test
    void testTokenSubjectValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusMinutes(30),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE, TEST_AUDIENCE, "different-subject")
        );
    }

    @Test
    void testInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";
        Assertions.assertThrows(SecurityException.class, () ->
            jwtService.validateToken(invalidToken, keyPair.getPublic(), ACCESS_TOKEN_TYPE)
        );
    }
}
