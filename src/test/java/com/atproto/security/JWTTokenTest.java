package com.atproto.security;

import com.atproto.security.jwt.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class JWTTokenTest {

    @InjectMocks
    private JWTService jwtService;

    private static final String TEST_AUDIENCE = "test-audience";
    private static final String TEST_SUBJECT = "test-subject";
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", "BC");
        keyPair = generator.generateKeyPair();
    }

    @Test
    void testTokenGenerationAndValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusSeconds(3600),
            keyPair.getPrivate()
        );

        assertTrue(jwtService.validateToken(token, keyPair.getPublic()));
    }

    @Test
    void testExpiredToken() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().minusSeconds(1),
            keyPair.getPrivate()
        );

        assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, keyPair.getPublic())
        );
    }

    @Test
    void testInvalidSignature() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusSeconds(3600),
            keyPair.getPrivate()
        );

        // Create a different key pair to simulate invalid signature
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", "BC");
        KeyPair invalidKeyPair = generator.generateKeyPair();

        assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, invalidKeyPair.getPublic())
        );
    }

    @Test
    void testInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";
        assertThrows(SecurityException.class, () ->
            jwtService.validateToken(invalidToken, keyPair.getPublic())
        );
    }

    @Test
    void testTokenAudienceValidation() throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusSeconds(3600),
            keyPair.getPrivate()
        );

        assertThrows(SecurityException.class, () ->
            jwtService.validateToken(token, keyPair.getPublic(), "different-audience")
        );
    }
}
