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
    void testSessionCreation() throws Exception {
        AuthSession session = sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN
        );

        assertNotNull(session);
        assertNotNull(session.getSessionToken());
        assertEquals(TEST_DID, session.getDID());
        assertEquals(TEST_PDS, session.getPDS());
    }

    @Test
    void testSessionRefresh() throws Exception {
        // Create initial session
        AuthSession initialSession = sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN
        );

        // Refresh session
        AuthSession refreshedSession = sessionManager.refreshSession(
            initialSession.getSessionToken()
        );

        assertNotNull(refreshedSession);
        assertNotEquals(initialSession.getSessionToken(), refreshedSession.getSessionToken());
        assertEquals(TEST_DID, refreshedSession.getDID());
    }

    @Test
    void testSessionInvalidation() throws Exception {
        // Create initial session
        AuthSession session = sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN
        );

        // Invalidate session
        sessionManager.invalidateSession(session.getSessionToken());

        // Attempt to refresh should fail
        assertThrows(SecurityException.class, () ->
            sessionManager.refreshSession(session.getSessionToken())
        );
    }

    @Test
    void testSessionExpiration() throws Exception {
        // Create session with short expiration
        AuthSession session = sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN,
            Instant.now().plusSeconds(1)
        );

        // Wait for expiration
        Thread.sleep(2000);

        // Attempt to refresh should fail
        assertThrows(SecurityException.class, () ->
            sessionManager.refreshSession(session.getSessionToken())
        );
    }

    @Test
    void testSessionPersistence() throws Exception {
        // Create session
        AuthSession session = sessionManager.createSession(
            TEST_DID,
            TEST_PASSWORD,
            TEST_PDS,
            TEST_AUTH_FACTOR_TOKEN
        );

        // Persist session
        sessionManager.persistSession(session);

        // Retrieve persisted session
        AuthSession retrievedSession = sessionManager.loadSession(session.getSessionToken());

        assertNotNull(retrievedSession);
        assertEquals(session.getSessionToken(), retrievedSession.getSessionToken());
        assertEquals(session.getDID(), retrievedSession.getDID());
    }
}
