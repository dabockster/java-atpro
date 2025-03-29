package com.atproto.api.errorhandling;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XrpcErrorHandlingTest {

    @Mock
    private XrpcClient xrpcClient;

    private XrpcErrorHandling errorHandling;

    @BeforeEach
    public void setUp() {
        errorHandling = new XrpcErrorHandling(xrpcClient);
    }

    @Test
    public void shouldHandleNotFoundErrorResponse() throws Exception {
        // Given
        String errorJson = "{\"error\":\"not_found\",\"message\":\"Resource not found\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(404, exception.getStatusCode());
        assertEquals("not_found", exception.getCode());
        assertEquals("Resource not found", exception.getMessage());
    }

    @Test
    public void shouldHandleNetworkError() throws Exception {
        // Given
        RuntimeException networkError = new RuntimeException("Network error");
        when(xrpcClient.sendRequest(any())).thenThrow(networkError);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.sendRequest();
        });

        assertTrue(exception.getMessage().contains("Network error"));
        assertTrue(exception.isNetworkError());
    }

    @Test
    public void shouldHandleTimeoutError() throws Exception {
        // Given
        TimeoutException timeoutError = new TimeoutException("Request timed out");
        when(xrpcClient.sendRequest(any())).thenThrow(timeoutError);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.sendRequest();
        });

        assertTrue(exception.getMessage().contains("timeout"));
        assertTrue(exception.isTimeout());
    }

    @Test
    public void shouldHandleValidationError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"invalid_request\",\"message\":\"Invalid parameter: value must be a string\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("invalid_request", exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid parameter"));
    }

    @Test
    public void shouldHandleMultipleErrors() throws Exception {
        // Given
        String errorJson = "{\"error\":\"validation_error\",\"message\":\"Multiple validation errors\",\"details\":[\"Field1 is required\",\"Field2 must be numeric\"]}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("validation_error", exception.getCode());
        assertTrue(exception.getMessage().contains("Multiple validation errors"));
        assertTrue(exception.getDetails().contains("Field1 is required"));
        assertTrue(exception.getDetails().contains("Field2 must be numeric"));
    }

    @Test
    public void shouldHandleRateLimiting() throws Exception {
        // Given
        String errorJson = "{\"error\":\"rate_limit_exceeded\",\"message\":\"Rate limit exceeded\",\"retry_after\":30}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(429);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(429, exception.getStatusCode());
        assertEquals("rate_limit_exceeded", exception.getCode());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        assertTrue(exception.getRetryAfter() > 0);
    }

    @Test
    public void shouldHandleAuthenticationError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"unauthorized\",\"message\":\"Invalid credentials\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(401, exception.getStatusCode());
        assertEquals("unauthorized", exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid credentials"));
    }

    @Test
    public void shouldHandleServerError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"internal_error\",\"message\":\"Server encountered an error\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(500, exception.getStatusCode());
        assertEquals("internal_error", exception.getCode());
        assertTrue(exception.getMessage().contains("Server encountered an error"));
    }

    @Test
    public void shouldHandleMalformedResponse() throws Exception {
        // Given
        String malformedJson = "{\"error\":\"invalid_json\",\"message\":Invalid JSON""";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(malformedJson);

        // When & Then
        XrpcException exception = assertThrows(XrpcException.class, () -> {
            errorHandling.handleResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid JSON"));
    }
}
