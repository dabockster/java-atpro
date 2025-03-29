package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import com.atproto.crypto.KeyPair;
import com.atproto.crypto.Secp256k1KeyPair;
import com.atproto.did.DID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionManagementTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private AuthSessionManager sessionManager;

    private static final String TEST_DID = "did:plc:1234567890abcdef";
    private static final String TEST_PASSWORD = "test-password";
    private static final String TEST_PDS = "https://bsky.social";
    private static final String TEST_AUTH_FACTOR_TOKEN = "123456";
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = Secp256k1KeyPair.create();
    }

    @Test
    void testSessionCreationWithValidParameters() throws Exception {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When
        AuthSession session = sessionManager.createSession(request);

        // Then
        assertThat(session).isNotNull();
        assertThat(session.getSessionToken()).isNotEmpty();
        assertThat(session.getDID()).isEqualTo(TEST_DID);
        assertThat(session.getPDS()).isEqualTo(TEST_PDS);
        assertThat(session.getExpiresAt()).isAfter(Instant.now());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid:did", "did:plc:invalid", "did:web:"})
    void testSessionCreationWithInvalidDid(String invalidDid) {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", invalidDid,
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThatThrownBy(() ->
            sessionManager.createSession(request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "123", "password"})
    void testSessionCreationWithInvalidPassword(String invalidPassword) {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", invalidPassword,
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThatThrownBy(() ->
            sessionManager.createSession(request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-url", "http://invalid", "https://invalid:"})
    void testSessionCreationWithInvalidPds(String invalidPds) {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", invalidPds,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThatThrownBy(() ->
            sessionManager.createSession(request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234", "12345", "1234567"})
    void testSessionCreationWithInvalidAuthFactorToken(String invalidToken) {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", invalidToken
        ));

        // When & Then
        assertThatThrownBy(() ->
            sessionManager.createSession(request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSessionRefreshWithValidToken() throws Exception {
        // Given
        AuthSession initialSession = createTestSession();
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", initialSession.getSessionToken()
        ));

        // When
        AuthSession refreshedSession = sessionManager.refreshSession(request);

        // Then
        assertNotNull(refreshedSession);
        assertNotEquals(initialSession.getSessionToken(), refreshedSession.getSessionToken());
        assertEquals(TEST_DID, refreshedSession.getDID());
        assertTrue(refreshedSession.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void testSessionRefreshWithExpiredToken() throws Exception {
        // Given
        AuthSession expiredSession = createTestSession();
        expiredSession.setExpiresAt(Instant.now().minusSeconds(1));
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", expiredSession.getSessionToken()
        ));

        // When & Then
        assertThrows(SecurityException.class, () ->
            sessionManager.refreshSession(request)
        );
    }

    @Test
    void testSessionRefreshWithInvalidToken() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", "invalid-token"
        ));

        // When & Then
        assertThrows(SecurityException.class, () ->
            sessionManager.refreshSession(request)
        );
    }

    @Test
    void testSessionInvalidation() throws Exception {
        // Given
        AuthSession session = createTestSession();
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", session.getSessionToken()
        ));

        // When
        sessionManager.invalidateSession(request);

        // Then
        assertThrows(SecurityException.class, () ->
            sessionManager.refreshSession(request)
        );
    }

    @Test
    void testSessionPersistence() throws Exception {
        // Given
        AuthSession session = createTestSession();
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", session.getSessionToken()
        ));

        // When
        sessionManager.persistSession(session);

        // Then
        AuthSession retrievedSession = sessionManager.loadSession(session.getSessionToken());
        assertNotNull(retrievedSession);
        assertEquals(session.getSessionToken(), retrievedSession.getSessionToken());
        assertEquals(session.getDID(), retrievedSession.getDID());
    }

    @Test
    void testSessionLoadingWithInvalidToken() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", "invalid-token"
        ));

        // When & Then
        assertThrows(SecurityException.class, () ->
            sessionManager.loadSession(request.getParams().get("sessionToken"))
        );
    }

    @Test
    void testSessionLoadingWithExpiredToken() throws Exception {
        // Given
        AuthSession expiredSession = createTestSession();
        expiredSession.setExpiresAt(Instant.now().minusSeconds(1));
        when(request.getParams()).thenReturn(Map.of(
            "sessionToken", expiredSession.getSessionToken()
        ));

        // When & Then
        assertThrows(SecurityException.class, () ->
            sessionManager.loadSession(request.getParams().get("sessionToken"))
        );
    }

    @Test
    void testSessionValidation() throws Exception {
        // Given
        AuthSession session = createTestSession();
        
        // When
        boolean isValid = sessionManager.validateSession(session.getSessionToken());

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void testSessionExpiration() throws Exception {
        // Given
        AuthSession session = createTestSession();
        Instant now = Instant.now();
        when(session.getExpiresAt()).thenReturn(now.minusSeconds(1));

        // When
        boolean isValid = sessionManager.validateSession(session.getSessionToken());

        // Then
        assertThat(isValid).isFalse();
    }

    private AuthSession createTestSession() throws Exception {
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));
        return sessionManager.createSession(request);
    }
}
