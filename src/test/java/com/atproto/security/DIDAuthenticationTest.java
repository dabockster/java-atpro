package com.atproto.security;

import com.atproto.did.DIDResolver;
import com.atproto.did.DIDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DIDAuthenticationTest {

    @Mock
    private DIDResolver didResolver;

    @InjectMocks
    private DIDAuthenticationService authenticationService;

    private static final String VALID_DID = "did:plc:1234567890abcdef";
    private static final String INVALID_DID = "invalid-did";

    @BeforeEach
    void setUp() {
        // Setup mock responses
        when(didResolver.resolveDID(VALID_DID)).thenReturn(createValidDIDDocument());
        when(didResolver.resolveDID(INVALID_DID)).thenReturn(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:plc:1234567890abcdef", "did:web:example.com"})
    void testValidDIDAuthentication(String validDid) {
        when(didResolver.resolveDID(validDid)).thenReturn(createValidDIDDocument());
        assertThat(authenticationService.authenticateDID(validDid)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-did", "did:plc:invalid", "did:web:"})
    void testInvalidDIDAuthentication(String invalidDid) {
        assertThrows(IllegalArgumentException.class, () -> 
            authenticationService.authenticateDID(invalidDid)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:plc:1234567890abcdef", "did:web:example.com"})
    void testDIDFormatValidation(String validDid) {
        assertThat(authenticationService.validateDIDFormat(validDid)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-did", "did:plc:invalid", "did:web:"})
    void testDIDFormatValidationFailure(String invalidDid) {
        assertThat(authenticationService.validateDIDFormat(invalidDid)).isFalse();
    }

    @Test
    void testDIDResolutionFailure() {
        when(didResolver.resolveDID(VALID_DID)).thenThrow(new RuntimeException("Resolution failed"));
        assertThatThrownBy(() -> authenticationService.authenticateDID(VALID_DID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Resolution failed");
    }

    private DIDDocument createValidDIDDocument() {
        return DIDDocument.builder()
            .id(VALID_DID)
            .verificationMethod(Arrays.asList(
                VerificationMethod.builder()
                    .id("#key-1")
                    .type("Ed25519VerificationKey2020")
                    .controller(VALID_DID)
                    .publicKeyMultibase("z6MkghvUk5NR6F3kq25mhbhVUUK7P8Hj9jUfyw4QL1Uejot")
                    .build()
            ))
            .authentication(Arrays.asList("#key-1"))
            .build();
    }
}
