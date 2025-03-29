package com.atproto.security;

import com.atproto.did.DIDResolver;
import com.atproto.did.DIDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.dependencycheck.utils.StringUtils;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void testValidDIDAuthentication() {
        assertTrue(authenticationService.authenticateDID(VALID_DID));
    }

    @Test
    void testInvalidDIDAuthentication() {
        assertThrows(IllegalArgumentException.class, () -> 
            authenticationService.authenticateDID(INVALID_DID)
        );
    }

    @Test
    void testDIDFormatValidation() {
        assertFalse(authenticationService.validateDIDFormat(INVALID_DID));
        assertTrue(authenticationService.validateDIDFormat(VALID_DID));
    }

    @Test
    void testDIDResolutionFailure() {
        when(didResolver.resolveDID(VALID_DID)).thenThrow(new RuntimeException("Resolution failed"));
        assertThrows(RuntimeException.class, () -> 
            authenticationService.authenticateDID(VALID_DID)
        );
    }

    private DIDDocument createValidDIDDocument() {
        return DIDDocument.builder()
            .id(VALID_DID)
            .verificationMethod(List.of(
                VerificationMethod.builder()
                    .id("#key-1")
                    .type("Ed25519VerificationKey2020")
                    .controller(VALID_DID)
                    .publicKeyMultibase("z6MkghvUk5NR6F3kq25mhbhVUUK7P8Hj9jUfyw4QL1Uejot")
                    .build()
            ))
            .authentication(List.of("#key-1"))
            .build();
    }
}
