package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import com.atproto.crypto.KeyPair;
import com.atproto.crypto.Secp256k1KeyPair;
import com.atproto.did.DID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        assertNotNull(session);
        assertNotNull(session.getSessionToken());
        assertEquals(TEST_DID, session.getDID());
        assertEquals(TEST_PDS, session.getPDS());
        assertTrue(session.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void testSessionCreationWithInvalidDid() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", "invalid:did",
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            sessionManager.createSession(request)
        );
    }

    @Test
    void testSessionCreationWithEmptyPassword() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", "",
            "pds", TEST_PDS,
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            sessionManager.createSession(request)
        );
    }

    @Test
    void testSessionCreationWithInvalidPds() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", "invalid:pds",
            "authFactorToken", TEST_AUTH_FACTOR_TOKEN
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            sessionManager.createSession(request)
        );
    }

    @Test
    void testSessionCreationWithInvalidAuthFactorToken() {
        // Given
        when(request.getParams()).thenReturn(Map.of(
            "did", TEST_DID,
            "password", TEST_PASSWORD,
            "pds", TEST_PDS,
            "authFactorToken", "123"
        ));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            sessionManager.createSession(request)
        );
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

    private AuthSession createTestSession() throws Exception {
        return sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN
        );
    }
}
