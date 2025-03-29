package com.atproto.security;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.errors.UnsupportedDomainError;
import com.atproto.api.xrpc.errors.InvalidHandleError;
import com.atproto.api.xrpc.errors.EmailValidationError;
import com.atproto.api.xrpc.errors.PasswordStrengthError;
import com.atproto.api.xrpc.errors.RateLimitError;
import com.atproto.api.xrpc.errors.AuthenticationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorHandlingTest {

    @Mock
    private XrpcRequest request;

    @InjectMocks
    private XrpcErrorHandler errorHandler;

    private static final String TEST_HANDLE = "admin.blah";
    private static final String TEST_EMAIL = "admin@test.com";
    private static final String TEST_PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Setup mock request
        when(request.getPath()).thenReturn("/xrpc/com.atproto.server.createAccount");
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", TEST_EMAIL,
            "password", TEST_PASSWORD
        ));
    }

    @Test
    void testUnsupportedDomainError() {
        UnsupportedDomainError error = assertThrows(UnsupportedDomainError.class, () -> {
            errorHandler.handleCreateAccount(request);
        });

        assertEquals("Unsupported domain: blah", error.getMessage());
    }

    @Test
    void testInvalidHandleError() {
        when(request.getBody()).thenReturn(Map.of(
            "handle", "invalid.handle",
            "email", TEST_EMAIL,
            "password", TEST_PASSWORD
        ));

        InvalidHandleError error = assertThrows(InvalidHandleError.class, () -> {
            errorHandler.handleCreateAccount(request);
        });

        assertTrue(error.getMessage().contains("Invalid handle format"));
    }

    @Test
    void testEmailValidationError() {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", "invalid-email",
            "password", TEST_PASSWORD
        ));

        EmailValidationError error = assertThrows(EmailValidationError.class, () -> {
            errorHandler.handleCreateAccount(request);
        });

        assertTrue(error.getMessage().contains("Invalid email format"));
    }

    @Test
    void testPasswordStrengthError() {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", TEST_EMAIL,
            "password", "weak"
        ));

        PasswordStrengthError error = assertThrows(PasswordStrengthError.class, () -> {
            errorHandler.handleCreateAccount(request);
        });

        assertTrue(error.getMessage().contains("Password is too weak"));
    }

    @Test
    void testRateLimitError() {
        RateLimitError error = assertThrows(RateLimitError.class, () -> {
            errorHandler.checkRateLimit(request);
        });

        assertTrue(error.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void testAuthenticationError() {
        AuthenticationError error = assertThrows(AuthenticationError.class, () -> {
            errorHandler.authenticate(request);
        });

        assertTrue(error.getMessage().contains("Authentication failed"));
    }
}
