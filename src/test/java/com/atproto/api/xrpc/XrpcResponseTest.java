package com.atproto.api.xrpc;

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
public class XrpcResponseTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private XrpcResponse xrpcResponse;

    private static final String TEST_METHOD = "com.atproto.sync.getLatestCommit";
    private static final String TEST_ERROR_CODE = "not_found";
    private static final String TEST_ERROR_MESSAGE = "Resource not found";

    @BeforeEach
    void setUp() {
        xrpcResponse = new XrpcResponse(TEST_METHOD);
    }

    @Test
    public void testResponseCreation() throws IOException {
        // Test basic response creation
        assertNotNull(xrpcResponse);
        assertEquals(TEST_METHOD, xrpcResponse.getMethod());

        // Test with data
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        assertEquals(data, xrpcResponse.getData());

        // Test with error
        XrpcResponse errorResponse = new XrpcResponse(TEST_METHOD);
        errorResponse.setError(TEST_ERROR_CODE, TEST_ERROR_MESSAGE);
        assertEquals(TEST_ERROR_CODE, errorResponse.getErrorCode());
        assertEquals(TEST_ERROR_MESSAGE, errorResponse.getErrorMessage());
    }

    @Test
    public void testResponseValidation() throws IOException {
        // Test valid response
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        assertTrue(xrpcResponse.validate());

        // Test invalid method name
        XrpcResponse invalidMethod = new XrpcResponse("invalid.method");
        assertFalse(invalidMethod.validate());

        // Test invalid data type
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("commit", new Object()); // Invalid type
        xrpcResponse.setData(invalidData);
        assertFalse(xrpcResponse.validate());

        // Test invalid error format
        XrpcResponse invalidError = new XrpcResponse(TEST_METHOD);
        invalidError.setError("invalid_code", ""); // Empty error message
        assertFalse(invalidError.validate());
    }

    @Test
    public void testResponseSerialization() throws IOException {
        // Test basic serialization
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);

        String serialized = xrpcResponse.serialize();
        assertNotNull(serialized);

        // Test deserialization
        JsonNode node = objectMapper.readTree(serialized);
        assertEquals(TEST_METHOD, node.get("method").asText());
        assertEquals("abc123", node.get("data").get("commit").asText());

        // Test error response serialization
        XrpcResponse errorResponse = new XrpcResponse(TEST_METHOD);
        errorResponse.setError(TEST_ERROR_CODE, TEST_ERROR_MESSAGE);
        String errorSerialized = errorResponse.serialize();
        assertNotNull(errorSerialized);

        JsonNode errorNode = objectMapper.readTree(errorSerialized);
        assertEquals(TEST_METHOD, errorNode.get("method").asText());
        assertEquals(TEST_ERROR_CODE, errorNode.get("error").get("code").asText());
        assertEquals(TEST_ERROR_MESSAGE, errorNode.get("error").get("message").asText());

        // Test empty response serialization
        XrpcResponse emptyResponse = new XrpcResponse(TEST_METHOD);
        String emptySerialized = emptyResponse.serialize();
        assertNotNull(emptySerialized);
    }

    @Test
    public void testResponseErrorHandling() {
        // Test null method
        assertThrows(NullPointerException.class, () -> {
            new XrpcResponse(null);
        });

        // Test empty method
        assertThrows(IllegalArgumentException.class, () -> {
            new XrpcResponse("");
        });

        // Test invalid error code
        XrpcResponse invalidError = new XrpcResponse(TEST_METHOD);
        invalidError.setError("", "error message"); // Empty code
        assertFalse(invalidError.validate());

        // Test invalid data type
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("commit", new Object()); // Invalid type
        xrpcResponse.setData(invalidData);
        assertFalse(xrpcResponse.validate());

        // Test serialization of invalid response
        XrpcResponse invalidResponse = new XrpcResponse("invalid.method");
        assertThrows(IllegalArgumentException.class, () -> {
            invalidResponse.serialize();
        });
    }

    @Test
    public void testResponseWithHeaders() throws IOException {
        // Test adding headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        xrpcResponse.setHeaders(headers);
        assertEquals(headers, xrpcResponse.getHeaders());

        // Test invalid headers
        Map<String, String> invalidHeaders = new HashMap<>();
        invalidHeaders.put("Content-Type", "invalid/type");
        xrpcResponse.setHeaders(invalidHeaders);
        assertFalse(xrpcResponse.validate());

        // Test serialization with headers
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        xrpcResponse.setHeaders(headers);

        String serialized = xrpcResponse.serialize();
        assertNotNull(serialized);

        JsonNode node = objectMapper.readTree(serialized);
        assertEquals("application/json", node.get("headers").get("Content-Type").asText());
    }
}
