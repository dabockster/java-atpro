package com.atproto.errorhandling;

import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.errors.UnsupportedDomainError;
import com.atproto.api.xrpc.errors.InvalidHandleError;
import com.atproto.api.xrpc.errors.EmailValidationError;
import com.atproto.api.xrpc.errors.PasswordStrengthError;
import com.atproto.api.xrpc.errors.RateLimitError;
import com.atproto.api.xrpc.errors.AuthenticationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(UnsupportedDomainError.class)
            .hasMessage("Unsupported domain: blah");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid.handle", "handle-with-space", "handle_with_underscore"})
    void testInvalidHandleError(String invalidHandle) {
        when(request.getBody()).thenReturn(Map.of(
            "handle", invalidHandle,
            "email", TEST_EMAIL,
            "password", TEST_PASSWORD
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(InvalidHandleError.class)
            .hasMessageContaining("Invalid handle format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "noatsign.com", "@nodomain"})
    void testEmailValidationError(String invalidEmail) {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", invalidEmail,
            "password", TEST_PASSWORD
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(EmailValidationError.class)
            .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"weak", "123456", "password", "qwerty"})
    void testPasswordStrengthError(String weakPassword) {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", TEST_EMAIL,
            "password", weakPassword
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(PasswordStrengthError.class)
            .hasMessageContaining("Password is too weak");
    }

    @Test
    void testRateLimitError() {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", TEST_EMAIL,
            "password", TEST_PASSWORD
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(RateLimitError.class)
            .hasMessageContaining("Rate limit exceeded");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid_token", "expired_token", "revoked_token"})
    void testAuthenticationError(String invalidToken) {
        when(request.getBody()).thenReturn(Map.of(
            "handle", TEST_HANDLE,
            "email", TEST_EMAIL,
            "password", TEST_PASSWORD,
            "token", invalidToken
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(AuthenticationError.class)
            .hasMessageContaining("Authentication failed");
    }

    @Test
    void testMultipleErrors() {
        when(request.getBody()).thenReturn(Map.of(
            "handle", "invalid.handle",
            "email", "invalid-email",
            "password", "weak"
        ));

        assertThatThrownBy(() -> errorHandler.handleCreateAccount(request))
            .isInstanceOf(InvalidHandleError.class)
            .hasMessageContaining("Invalid handle format");
    }
}
