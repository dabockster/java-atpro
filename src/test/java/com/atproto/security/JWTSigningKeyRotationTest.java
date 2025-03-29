package com.atproto.security;

import com.atproto.crypto.KeyPair;
import com.atproto.crypto.Secp256k1KeyPair;
import com.atproto.did.DID;
import com.atproto.did.DIDDocument;
import com.atproto.did.DIDResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(jwtService.validateToken(originalToken, originalKeyPair)).isTrue();

        // Update DID document with new key
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(newKeyPair));

        // Token should be invalid with new key
        assertThat(jwtService.validateToken(originalToken, newKeyPair)).isFalse();

        // Generate new token with new key
        String newToken = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            Instant.now().plusSeconds(3600),
            newKeyPair
        );

        // New token should be valid with new key
        assertThat(jwtService.validateToken(newToken, newKeyPair)).isTrue();
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
        assertThat(jwtService.validateToken(originalToken, originalKeyPair)).isTrue();

        // Update DID document with new key
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(newKeyPair));

        // Token should still be valid due to cache
        assertThat(jwtService.validateToken(originalToken, originalKeyPair)).isTrue();

        // After cache expiration, token should be invalid
        jwtService.clearCache();
        assertThat(jwtService.validateToken(originalToken, originalKeyPair)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "3600", "7200"})
    void testTokenExpiration(String seconds) throws Exception {
        int secondsValue = Integer.parseInt(seconds);
        Instant expirationTime = Instant.now().plusSeconds(secondsValue);
        
        String token = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            expirationTime,
            originalKeyPair
        );
        
        assertThat(jwtService.validateToken(token, originalKeyPair))
            .isEqualTo(expirationTime.isAfter(Instant.now()));
    }

    @Test
    void testMultipleKeyRotations() throws Exception {
        // Generate initial token
        String initialToken = jwtService.generateToken(
            TEST_DID,
            "test-audience",
            Instant.now().plusSeconds(3600),
            originalKeyPair
        );
        
        // First rotation
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(newKeyPair));
        
        // Second rotation
        KeyPair secondKeyPair = Secp256k1KeyPair.create();
        when(didResolver.resolveDID(TEST_DID)).thenReturn(createValidDIDDocument(secondKeyPair));
        
        // Token should be invalid with both new keys
        assertThat(jwtService.validateToken(initialToken, newKeyPair)).isFalse();
        assertThat(jwtService.validateToken(initialToken, secondKeyPair)).isFalse();
    }

    private DIDDocument createValidDIDDocument(KeyPair keyPair) {
        return new DIDDocument.Builder()
            .id(TEST_DID)
            .verificationMethod(List.of(
                new DIDDocument.VerificationMethod(
                    TEST_DID + "#key-1",
                    "Ed25519VerificationKey2020",
                    keyPair.getPublicKey().getEncoded()
                )
            ))
            .build();
    }
}
