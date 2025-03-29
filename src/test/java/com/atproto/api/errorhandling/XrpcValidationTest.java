package com.atproto.api.errorhandling;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.lexicon.LexiconSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XrpcValidationTest {

    @Mock
    private XrpcClient xrpcClient;

    @Mock
    private LexiconSchema schema;

    private XrpcValidation validation;

    @BeforeEach
    public void setUp() {
        validation = new XrpcValidation(xrpcClient, schema);
    }

    @Test
    public void shouldValidateValidRequestParameters() throws XrpcException {
        // Given
        Map<String, Object> validParams = Map.of(
            "requiredField", "value",
            "optionalField", "optional"
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(validParams);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldThrowExceptionForInvalidRequestParameters() {
        // Given
        Map<String, Object> invalidParams = Map.of(
            "requiredField", null,
            "invalidType", 123
        );

        when(schema.validateRequest(any())).thenThrow(new XrpcException(400, "invalid_request", "Invalid parameters"));

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> validation.validateRequest(invalidParams))
            .isInstanceOf(XrpcException.class)
            .hasMessage("Invalid parameters")
            .satisfies(e -> {
                Assertions.assertThat(e.getStatusCode()).isEqualTo(400);
                Assertions.assertThat(e.getCode()).isEqualTo("invalid_request");
            });

        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateValidResponse() throws XrpcException {
        // Given
        Map<String, Object> validResponse = Map.of(
            "data", Map.of(
                "id", "123",
                "value", "test"
            )
        );

        when(schema.validateResponse(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateResponse(validResponse);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateResponse(any());
    }

    @Test
    public void shouldThrowExceptionForInvalidResponse() {
        // Given
        Map<String, Object> invalidResponse = Map.of(
            "data", Map.of(
                "missingField", "value"
            )
        );

        when(schema.validateResponse(any())).thenThrow(new XrpcException(400, "invalid_response", "Invalid response format"));

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> validation.validateResponse(invalidResponse))
            .isInstanceOf(XrpcException.class)
            .hasMessage("Invalid response format")
            .satisfies(e -> {
                Assertions.assertThat(e.getStatusCode()).isEqualTo(400);
                Assertions.assertThat(e.getCode()).isEqualTo("invalid_response");
            });

        verify(schema).validateResponse(any());
    }

    @Test
    public void shouldValidateRequestWithArrayParameters() throws XrpcException {
        // Given
        Map<String, Object> requestWithArray = Map.of(
            "ids", List.of("123", "456"),
            "options", Map.of(
                "limit", 50,
                "offset", 0
            )
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(requestWithArray);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateRequestWithNestedObjects() throws XrpcException {
        // Given
        Map<String, Object> requestWithNestedObjects = Map.of(
            "user", Map.of(
                "id", "123",
                "profile", Map.of(
                    "name", "John Doe",
                    "bio", "Developer"
                )
            )
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(requestWithNestedObjects);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateResponseWithPagination() throws XrpcException {
        // Given
        Map<String, Object> responseWithPagination = Map.of(
            "data", List.of(
                Map.of("id", "123", "value", "test1"),
                Map.of("id", "456", "value", "test2")
            ),
            "cursor", "next_cursor_value"
        );

        when(schema.validateResponse(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateResponse(responseWithPagination);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateResponse(any());
    }

    @Test
    public void shouldValidateResponseWithErrors() throws XrpcException {
        // Given
        Map<String, Object> responseWithErrors = Map.of(
            "errors", List.of(
                Map.of("code", "invalid_value", "message", "Invalid value provided"),
                Map.of("code", "missing_field", "message", "Required field is missing")
            )
        );

        when(schema.validateResponse(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateResponse(responseWithErrors);

        // Then
        Assertions.assertThat(isValid).isTrue();
        verify(schema).validateResponse(any());
    }
}
