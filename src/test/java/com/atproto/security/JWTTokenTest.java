package com.atproto.security;

import com.atproto.security.jwt.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Assertions;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest
    @ValueSource(strings = {"test-audience", "other-audience"})
    void testAccessTokenGenerationAndValidation(String audience) throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            audience,
            Instant.now().plusMinutes(30),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        assertThat(jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"test-audience", "other-audience"})
    void testRefreshTokenGenerationAndValidation(String audience) throws Exception {
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            audience,
            Instant.now().plusHours(24),
            keyPair.getPrivate(),
            REFRESH_TOKEN_TYPE
        );

        assertThat(jwtService.validateToken(token, keyPair.getPublic(), REFRESH_TOKEN_TYPE)).isTrue();
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

        assertThatThrownBy(() ->
            jwtService.validateToken(accessToken, keyPair.getPublic(), REFRESH_TOKEN_TYPE)
        ).isInstanceOf(SecurityException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "23"})
    void testTokenLifetimeValidation(String hours) throws Exception {
        int lifetimeHours = Integer.parseInt(hours);
        String token = jwtService.generateToken(
            TEST_SUBJECT,
            TEST_AUDIENCE,
            Instant.now().plusHours(lifetimeHours),
            keyPair.getPrivate(),
            ACCESS_TOKEN_TYPE
        );

        if (lifetimeHours > 30) {
            assertThatThrownBy(() ->
                jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE)
            ).isInstanceOf(SecurityException.class);
        } else {
            assertThat(jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE)).isTrue();
        }
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

        assertThatThrownBy(() ->
            jwtService.validateToken(token, differentKeyPair.getPublic(), ACCESS_TOKEN_TYPE)
        ).isInstanceOf(SecurityException.class);
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

        assertThatThrownBy(() ->
            jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE, "different-audience")
        ).isInstanceOf(SecurityException.class);
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

        assertThatThrownBy(() ->
            jwtService.validateToken(token, keyPair.getPublic(), ACCESS_TOKEN_TYPE, TEST_AUDIENCE, "different-subject")
        ).isInstanceOf(SecurityException.class);
    }

    @Test
    void testInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";
        assertThatThrownBy(() ->
            jwtService.validateToken(invalidToken, keyPair.getPublic(), ACCESS_TOKEN_TYPE)
        ).isInstanceOf(SecurityException.class);
    }
}
