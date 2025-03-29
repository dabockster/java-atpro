package com.atproto.api.errorhandling;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.lexicon.LexiconSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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
        assertTrue(isValid);
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
        assertThrows(XrpcException.class, () -> validation.validateRequest(invalidParams));
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
        assertTrue(isValid);
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
        assertThrows(XrpcException.class, () -> validation.validateResponse(invalidResponse));
    }

    @Test
    public void shouldValidateNestedObjectStructure() throws XrpcException {
        // Given
        Map<String, Object> nestedParams = Map.of(
            "outer", Map.of(
                "inner", Map.of(
                    "value", "valid"
                )
            )
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(nestedParams);

        // Then
        assertTrue(isValid);
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateArrayStructure() throws XrpcException {
        // Given
        Map<String, Object> arrayParams = Map.of(
            "items", List.of(
                Map.of("id", "1"),
                Map.of("id", "2")
            )
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(arrayParams);

        // Then
        assertTrue(isValid);
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateEnumValues() throws XrpcException {
        // Given
        Map<String, Object> enumParams = Map.of(
            "status", "active"
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(enumParams);

        // Then
        assertTrue(isValid);
        verify(schema).validateRequest(any());
    }

    @Test
    public void shouldValidateCustomTypeStructure() throws XrpcException {
        // Given
        Map<String, Object> customTypeParams = Map.of(
            "customType", Map.of(
                "type", "custom",
                "value", "123"
            )
        );

        when(schema.validateRequest(any())).thenReturn(true);

        // When
        boolean isValid = validation.validateRequest(customTypeParams);

        // Then
        assertTrue(isValid);
        verify(schema).validateRequest(any());
    }
}
