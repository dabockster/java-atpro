package com.atproto.api.errorhandling;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.api.xrpc.XrpcResponse;
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

    private XrpcErrorHandlingTest subject;

    @BeforeEach
    public void setUp() {
        subject = new XrpcErrorHandlingTest();
    }

    @Test
    public void testXrpcErrorResponse() throws Exception {
        // Test error response with code and message
        String errorJson = "{\"error\":\"not_found\",\"message\":\"Resource not found\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(404, exception.getStatusCode());
        assertEquals("not_found", exception.getCode());
        assertEquals("Resource not found", exception.getMessage());
    }

    @Test
    public void testNetworkError() throws Exception {
        // Test network error handling
        when(xrpcClient.sendRequest(any())).thenThrow(new RuntimeException("Network error"));

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.sendRequest();
        });

        assertTrue(exception.getMessage().contains("Network error"));
        assertTrue(exception.isNetworkError());
    }

    @Test
    public void testTimeoutError() throws Exception {
        // Test timeout handling
        when(xrpcClient.sendRequest(any())).thenThrow(new TimeoutException("Request timed out"));

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.sendRequest();
        });

        assertTrue(exception.getMessage().contains("timeout"));
        assertTrue(exception.isTimeout());
    }

    @Test
    public void testValidationErrorResponse() throws Exception {
        // Test validation error response
        String errorJson = "{\"error\":\"invalid_request\",\"message\":\"Invalid parameter: value must be a string\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("invalid_request", exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid parameter"));
    }

    @Test
    public void testMultipleErrorResponses() throws Exception {
        // Test handling multiple errors in a single response
        String errorJson = "{\"error\":\"validation_error\",\"message\":\"Multiple validation errors\",\"details\":[\"Field1 is required\",\"Field2 must be numeric\"]}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("validation_error", exception.getCode());
        assertTrue(exception.getMessage().contains("Multiple validation errors"));
        assertTrue(exception.getDetails().contains("Field1 is required"));
        assertTrue(exception.getDetails().contains("Field2 must be numeric"));
    }

    @Test
    public void testRateLimitingError() throws Exception {
        // Test rate limiting error (429)
        String errorJson = "{\"error\":\"rate_limit_exceeded\",\"message\":\"Rate limit exceeded\",\"retry_after\":30}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(429);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(429, exception.getStatusCode());
        assertEquals("rate_limit_exceeded", exception.getCode());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        assertTrue(exception.getRetryAfter() > 0);
    }

    @Test
    public void testAuthenticationError() throws Exception {
        // Test authentication error (401/403)
        String errorJson = "{\"error\":\"unauthorized\",\"message\":\"Invalid credentials\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(401, exception.getStatusCode());
        assertEquals("unauthorized", exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid credentials"));
    }

    @Test
    public void testServerError() throws Exception {
        // Test server error (500+)
        String errorJson = "{\"error\":\"internal_error\",\"message\":\"Server encountered an error\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(errorJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(500, exception.getStatusCode());
        assertEquals("internal_error", exception.getCode());
        assertTrue(exception.getMessage().contains("Server encountered an error"));
    }

    @Test
    public void testMalformedResponse() throws Exception {
        // Test handling of malformed response
        String malformedJson = "{\"error\":\"invalid_json\",\"message\":Invalid JSON""";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(malformedJson);

        XrpcException exception = assertThrows(XrpcException.class, () -> {
            subject.handleXrpcResponse(response);
        });

        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid JSON"));
    }

    // Helper methods for testing
    private void handleXrpcResponse(HttpResponse<String> response) throws Exception {
        // Implementation would handle the response and throw appropriate exceptions
    }

    private void sendRequest() throws Exception {
        // Implementation would send the request and handle errors
    }
}
