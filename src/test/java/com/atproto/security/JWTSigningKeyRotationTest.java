package com.atproto.security;

import com.atproto.crypto.KeyPair;
import com.atproto.crypto.Secp256k1KeyPair;
import com.atproto.did.DID;
import com.atproto.did.DIDDocument;
import com.atproto.did.DIDResolver;
import com.atproto.security.jwt.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTSigningKeyRotationTest {

    @Mock
    private DIDResolver didResolver;

    @InjectMocks
    private JWTService jwtService;

    private static final String TEST_DID = "did:plc:1234567890abcdef";
    private KeyPair originalKeyPair;
    private KeyPair newKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        
        // Create original key pair
        originalKeyPair = Secp256k1KeyPair.create();
        
        // Create new key pair for rotation
        newKeyPair = Secp256k1KeyPair.create();
        
        // Setup mock DID resolver
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(originalKeyPair));
    }

    @Test
    void testKeyRotationSuccess() throws Exception {
        // Generate token with original key
        String originalToken = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            Instant.now().plusSeconds(3600),
            originalKeyPair
        );

        // Token should be valid with original key
        assertTrue(jwtService.validateToken(originalToken, originalKeyPair));

        // Update DID document with new key
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(newKeyPair));

        // Token should be invalid with new key
        assertFalse(jwtService.validateToken(originalToken, newKeyPair));

        // Generate new token with new key
        String newToken = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            Instant.now().plusSeconds(3600),
            newKeyPair
        );

        // New token should be valid with new key
        assertTrue(jwtService.validateToken(newToken, newKeyPair));
    }

    @Test
    void testKeyRotationCacheBehavior() throws Exception {
        // Generate token with original key
        String originalToken = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            Instant.now().plusSeconds(3600),
            originalKeyPair
        );

        // Token should be valid with original key
        assertTrue(jwtService.validateToken(originalToken, originalKeyPair));

        // Update DID document with new key
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(newKeyPair));

        // Token should still be valid due to cache
        assertTrue(jwtService.validateToken(originalToken, originalKeyPair));

        // After cache expiration, token should be invalid
        jwtService.clearCache();
        assertFalse(jwtService.validateToken(originalToken, originalKeyPair));
    }

    private DIDDocument createValidDIDDocument(KeyPair keyPair) {
        return DIDDocument.builder()
            .id(TEST_DID)
            .verificationMethod(List.of(
                VerificationMethod.builder()
                    .id("#key-1")
                    .type("Ed25519VerificationKey2020")
                    .controller(TEST_DID)
                    .publicKeyMultibase(keyPair.getPublicKeyBase32())
                    .build()
            ))
            .authentication(List.of("#key-1"))
            .build();
    }
}
