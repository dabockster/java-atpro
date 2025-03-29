package com.atproto.did;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        // Test successful resolution
        JsonNode resolvedDoc = didResolver.resolve(TEST_DID);
        assertThat(resolvedDoc).isNotNull();
        assertThat(resolvedDoc.get("id").asText()).isEqualTo(TEST_DID);

        // Test invalid DID format using AssertJ
        assertThatThrownBy(() -> didResolver.resolve("invalid_did_format"))
            .isInstanceOf(IllegalArgumentException.class);

        // Test non-existent DID
        assertThatThrownBy(() -> didResolver.resolve("did:plc:nonexistent"))
            .isInstanceOf(DidResolutionException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "did:plc:123, true",
        "did:plc:invalid, false",
        "did:web:example.com, true",
        "did:key:z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av, true",
        "did:web:example.com/path, false",
        "did:example:123, false"
    })
    public void testDidFormatValidation(String did, boolean expected) {
        assertThat(didResolver.validateDidFormat(did)).isEqualTo(expected);
    }

    @Test
    public void testDidDocumentValidation() throws IOException {
        // Test valid DID document
        assertThat(didResolver.validateDidDocument(TEST_DID_DOC)).isTrue();

        // Test invalid DID document (missing id)
        String invalidDoc = "{\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}]}";
        assertThat(didResolver.validateDidDocument(invalidDoc)).isFalse();

        // Test invalid DID document (invalid verification method)
        String invalidVerificationDoc = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"InvalidType\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}]}";
        assertThat(didResolver.validateDidDocument(invalidVerificationDoc)).isFalse();

        // Test valid DID document with service endpoints
        String docWithService = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}],\"service\":[{\"id\":\"#atp\",\"type\":\"atproto\",\"serviceEndpoint\":\"https://bsky.social\"}]}";
        assertThat(didResolver.validateDidDocument(docWithService)).isTrue();
    }

    @Test
    public void testDidAuth() throws IOException {
        // Test successful authentication
        assertThat(didResolver.authenticate(TEST_DID, "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av")).isTrue();

        // Test failed authentication (wrong key)
        assertThat(didResolver.authenticate(TEST_DID, "invalid_key")).isFalse();

        // Test failed authentication (non-existent DID)
        assertThatThrownBy(() -> didResolver.authenticate("did:plc:nonexistent", "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av"))
            .isInstanceOf(DidAuthenticationException.class);

        // Test authentication with different key type
        String ed25519Key = "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av";
        String secp256k1Key = "z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av";
        assertThat(didResolver.authenticate(TEST_DID, ed25519Key)).isTrue();
        assertThat(didResolver.authenticate(TEST_DID, secp256k1Key)).isTrue();
    }

    @Test
    public void testDidErrorHandling() {
        // Test DID resolution error
        assertThatThrownBy(() -> didResolver.resolve("did:plc:invalid"))
            .isInstanceOf(DidResolutionException.class);

        // Test DID document parsing error
        assertThatThrownBy(() -> didResolver.validateDidDocument("invalid_json"))
            .isInstanceOf(DidDocumentParseException.class);

        // Test authentication error
        assertThatThrownBy(() -> didResolver.authenticate("did:plc:invalid", "key"))
            .isInstanceOf(DidAuthenticationException.class);

        // Test cache-related errors
        assertThatThrownBy(() -> didResolver.resolveFromCache("did:plc:expired"))
            .isInstanceOf(DidCacheException.class);
    }

    @Test
    public void testSecurityVulnerabilities() {
        // Test for potential security vulnerabilities
        String maliciousInput = "did:plc:123\";alert('XSS');\"";
        assertThat(didResolver.resolve(maliciousInput)).isNull();

        String invalidJson = "{\"id\":\"did:plc:123\",\"verificationMethod\":[]}";
        assertThat(didResolver.validateDidDocument(invalidJson)).isFalse();

        // Test for signature verification
        String docWithInvalidSignature = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}],\"signature\":\"invalid_signature\"}";
        assertThat(didResolver.validateDidDocument(docWithInvalidSignature)).isFalse();
    }

    @Test
    public void testDidDocumentVersioning() {
        // Test document versioning
        String v1Doc = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}],\"version\":\"1\"}";
        String v2Doc = "{\"id\":\"did:plc:123\",\"verificationMethod\":[{\"id\":\"#key1\",\"type\":\"Ed25519VerificationKey2020\",\"controller\":\"did:plc:123\",\"publicKeyMultibase\":\"z6MkghvF5svAmD2NBGntQHPdGjcS6a6t9QjfQwd4UDmYx8Av\"}],\"version\":\"2\"}";
        
        assertThat(didResolver.validateDidDocument(v1Doc)).isTrue();
        assertThat(didResolver.validateDidDocument(v2Doc)).isTrue();
        assertThat(didResolver.compareDocumentVersions(v1Doc, v2Doc)).isTrue();
    }
}
