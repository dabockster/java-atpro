package com.atproto.api.xrpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.Assertions;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XrpcClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private XrpcClient xrpcClient;

    private static final String TEST_ENDPOINT = "https://bsky.social/xrpc/com.atproto.sync.subscribeRepos";
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_DID = "did:plc:123";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }

    @Test
    void testSuccessfulQueryRequest() throws Exception {
        // Arrange
        String method = "com.atproto.sync.subscribeRepos";
        Map<String, Object> params = Map.of(
            "did", TEST_DID,
            "cursor", -1
        );
        
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"data\": {}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act
        var result = xrpcClient.query(method, params);

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(200);
        Assertions.assertThat(result.getBody()).isNotEmpty();
    }

    @Test
    void testFailedQueryRequest() throws Exception {
        // Arrange
        String method = "com.atproto.sync.subscribeRepos";
        Map<String, Object> params = Map.of(
            "did", TEST_DID,
            "cursor", -1
        );
        
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": \"Bad Request\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> xrpcClient.query(method, params))
                .isInstanceOf(XrpcException.class)
                .hasMessageContaining("Bad Request");
    }

    @Test
    void testWebSocketConnection() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(101);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act
        var result = xrpcClient.connectWebSocket(TEST_ENDPOINT);

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(101);
    }

    @Test
    void testWebSocketWithCursor() throws Exception {
        // Arrange
        String endpointWithCursor = TEST_ENDPOINT + "?cursor=-1";
        when(httpResponse.statusCode()).thenReturn(101);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act
        var result = xrpcClient.connectWebSocket(endpointWithCursor);

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(101);
    }

    @Test
    void testAuthenticationWithDIDHeader() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"data\": {\"did\": \"" + TEST_DID + "\"}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act
        var result = xrpcClient.authenticate(TEST_TOKEN);

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getBody()).contains(TEST_DID);
    }

    @Test
    void testRequestTimeout() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(new RuntimeException("Timeout"));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> xrpcClient.query("test", Map.of()))
                .isInstanceOf(XrpcTimeoutException.class)
                .hasMessageContaining("Timeout");
    }

    @Test
    void testInvalidJsonResponse() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{invalid json}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> xrpcClient.query("test", Map.of()))
                .isInstanceOf(XrpcParseException.class)
                .hasMessageContaining("Invalid JSON");
    }

    @Test
    void testRateLimiting() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("{\"error\": \"Too Many Requests\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> xrpcClient.query("test", Map.of()))
                .isInstanceOf(XrpcRateLimitException.class)
                .hasMessageContaining("Too Many Requests");
    }

    @Test
    void testServerDown() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> xrpcClient.query("test", Map.of()))
                .isInstanceOf(XrpcConnectionException.class)
                .hasMessageContaining("Connection refused");
    }
}
