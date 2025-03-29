package com.atproto.did;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DidResolverTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DidResolver didResolver;

    private static final String TEST_DID = "did:plc:123";
    private static final String TEST_DID_DOC = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}]}";

    @BeforeEach
    void setUp() {
        didResolver = new DidResolver();
    }

    @Test
    public void testDidResolution() throws IOException {
        // Mock DID document
        JsonNode didDocNode = objectMapper.readTree(TEST_DID_DOC);
        when(objectMapper.readTree(TEST_DID_DOC)).thenReturn(didDocNode);

        // Test successful resolution
        JsonNode resolvedDoc = didResolver.resolve(TEST_DID);
        assertNotNull(resolvedDoc);
        assertEquals(TEST_DID, resolvedDoc.get("id").asText());

        // Test invalid DID format
        assertThrows(IllegalArgumentException.class, () -> {
            didResolver.resolve("invalid_did_format");
        });

        // Test non-existent DID
        assertThrows(DidResolutionException.class, () -> {
            didResolver.resolve("did:plc:nonexistent");
        });
    }

    @Test
    public void testDidDocumentValidation() throws IOException {
        // Test valid DID document
        assertTrue(didResolver.validateDidDocument(TEST_DID_DOC));

        // Test invalid DID document (missing id)
        String invalidDoc = "{\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}]}";
        assertFalse(didResolver.validateDidDocument(invalidDoc));

        // Test invalid DID document (invalid verification method)
        String invalidVerificationDoc = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"InvalidType\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}]}";
        assertFalse(didResolver.validateDidDocument(invalidVerificationDoc));
    }

    @Test
    public void testDidAuth() throws IOException {
        // Test successful authentication
        assertTrue(didResolver.authenticate(TEST_DID, "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av"));

        // Test failed authentication (wrong key)
        assertFalse(didResolver.authenticate(TEST_DID, "invalid_key"));

        // Test failed authentication (non-existent DID)
        assertThrows(DidAuthenticationException.class, () -> {
            didResolver.authenticate("did:plc:nonexistent", "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av");
        });
    }

    @Test
    public void testDidErrorHandling() {
        // Test DID resolution error
        assertThrows(DidResolutionException.class, () -> {
            didResolver.resolve("did:plc:invalid");
        });

        // Test DID document parsing error
        assertThrows(DidDocumentParseException.class, () -> {
            didResolver.validateDidDocument("invalid_json");
        });

        // Test authentication error
        assertThrows(DidAuthenticationException.class, () -> {
            didResolver.authenticate("did:plc:invalid", "key");
        });
    }
}
