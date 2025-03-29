package com.atproto.api.xrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@ExtendWith(PowerMockRunnerDelegate.class)
@PrepareForTest({XrpcResponse.class})
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
        assertThat(xrpcResponse).isNotNull();
        assertThat(xrpcResponse.getMethod()).isEqualTo(TEST_METHOD);

        // Test with data
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        assertThat(xrpcResponse.getData()).isEqualTo(data);

        // Test with error
        XrpcResponse errorResponse = new XrpcResponse(TEST_METHOD);
        errorResponse.setError(TEST_ERROR_CODE, TEST_ERROR_MESSAGE);
        assertThat(errorResponse.getErrorCode()).isEqualTo(TEST_ERROR_CODE);
        assertThat(errorResponse.getErrorMessage()).isEqualTo(TEST_ERROR_MESSAGE);
    }

    @Test
    public void testResponseValidation() throws IOException {
        // Test valid response
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        assertThat(xrpcResponse.validate()).isTrue();

        // Test invalid method name
        XrpcResponse invalidMethod = new XrpcResponse("invalid.method");
        assertThat(invalidMethod.validate()).isFalse();

        // Test invalid data type
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("commit", new Object()); // Invalid type
        xrpcResponse.setData(invalidData);
        assertThat(xrpcResponse.validate()).isFalse();

        // Test invalid error format
        XrpcResponse invalidError = new XrpcResponse(TEST_METHOD);
        invalidError.setError("invalid_code", ""); // Empty error message
        assertThat(invalidError.validate()).isFalse();
    }

    @Test
    public void testResponseSerialization() throws IOException {
        // Test basic serialization
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);

        String serialized = xrpcResponse.serialize();
        assertThat(serialized).isNotNull();

        // Test deserialization
        JsonNode node = objectMapper.readTree(serialized);
        assertThat(node.get("method").asText()).isEqualTo(TEST_METHOD);
        assertThat(node.get("data").get("commit").asText()).isEqualTo("abc123");

        // Test error response serialization
        XrpcResponse errorResponse = new XrpcResponse(TEST_METHOD);
        errorResponse.setError(TEST_ERROR_CODE, TEST_ERROR_MESSAGE);
        String errorSerialized = errorResponse.serialize();
        assertThat(errorSerialized).isNotNull();

        JsonNode errorNode = objectMapper.readTree(errorSerialized);
        assertThat(errorNode.get("method").asText()).isEqualTo(TEST_METHOD);
        assertThat(errorNode.get("error").get("code").asText()).isEqualTo(TEST_ERROR_CODE);
        assertThat(errorNode.get("error").get("message").asText()).isEqualTo(TEST_ERROR_MESSAGE);
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
        assertThat(invalidError.validate()).isFalse();

        // Test invalid data type
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("commit", new Object()); // Invalid type
        xrpcResponse.setData(invalidData);
        assertThat(xrpcResponse.validate()).isFalse();

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
        assertThat(xrpcResponse.getHeaders()).isEqualTo(headers);

        // Test invalid headers
        Map<String, String> invalidHeaders = new HashMap<>();
        invalidHeaders.put("Content-Type", "invalid/type");
        xrpcResponse.setHeaders(invalidHeaders);
        assertThat(xrpcResponse.validate()).isFalse();

        // Test serialization with headers
        Map<String, Object> data = new HashMap<>();
        data.put("commit", "abc123");
        xrpcResponse.setData(data);
        xrpcResponse.setHeaders(headers);

        String serialized = xrpcResponse.serialize();
        assertThat(serialized).isNotNull();

        JsonNode node = objectMapper.readTree(serialized);
        assertThat(node.get("headers").get("Content-Type").asText()).isEqualTo("application/json");
    }
}
