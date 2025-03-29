package com.atproto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.Assertions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ClientTest {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private Client client;

    private static final String TEST_HOST = "https://bsky.social";
    private static final String TEST_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        client = new Client(TEST_HOST, TEST_TOKEN);
    }

    @Test
    void testCreateClientWithValidParameters() {
        Client testClient = new Client(TEST_HOST, TEST_TOKEN);
        assertThat(testClient.getHost()).isEqualTo(TEST_HOST);
        assertThat(testClient.getToken()).isEqualTo(TEST_TOKEN);
    }

    @Test
    void testCreateClientWithInvalidHost() {
        Assertions.assertThatThrownBy(() -> new Client("invalid-host", TEST_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid host URL format");
    }

    @Test
    void testCreateClientWithEmptyToken() {
        Assertions.assertThatThrownBy(() -> new Client(TEST_HOST, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token cannot be empty");
    }

    @Test
    void testSuccessfulXrpcRequest() throws Exception {
        String testMethod = "com.atproto.identity.resolveHandle";
        String testHandle = "test.handle";
        String testResponse = "{\"did\":\"did:plc:123\"}";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        when(httpResponse.body())
                .thenReturn(testResponse);

        when(httpResponse.statusCode())
                .thenReturn(200);

        Map<String, Object> result = client.sendXrpcRequest(testMethod, Map.of("handle", testHandle));

        assertThat(result).containsKey("did");
        assertThat(result.get("did")).isEqualTo("did:plc:123");
    }

    @Test
    void testXrpcRequestWithInvalidMethod() {
        Assertions.assertThatThrownBy(() -> client.sendXrpcRequest("invalid.method", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid XRPC method");
    }

    @Test
    void testXrpcRequestWithHttpError() throws Exception {
        String testMethod = "com.atproto.identity.resolveHandle";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        when(httpResponse.statusCode())
                .thenReturn(404);

        Assertions.assertThatThrownBy(() -> client.sendXrpcRequest(testMethod, Map.of()))
                .isInstanceOf(ClientException.class)
                .hasMessageContaining("HTTP 404");
    }

    @Test
    void testXrpcRequestWithInvalidResponse() throws Exception {
        String testMethod = "com.atproto.identity.resolveHandle";
        String invalidJson = "{invalid json}";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        when(httpResponse.body())
                .thenReturn(invalidJson);

        when(httpResponse.statusCode())
                .thenReturn(200);

        Assertions.assertThatThrownBy(() -> client.sendXrpcRequest(testMethod, Map.of()))
                .isInstanceOf(ClientException.class)
                .hasMessageContaining("Invalid JSON response");
    }

    @Test
    void testSubscribeToEvents() throws Exception {
        // This test would need to be implemented with actual event streaming
        // For now, we'll just verify the method exists
        Assertions.assertThatCode(() -> client.subscribeToEvents(event -> {
            // Event handler
        })).doesNotThrowAnyException();
    }

    @Test
    void testRepositoryOperations() throws Exception {
        String testDid = "did:plc:123";
        String testCid = "bafybeiaxwqy3535353535353535353535353535353535353535353535353535353535353535";

        // Test createRecord
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        when(httpResponse.body())
                .thenReturn("{\"cid\":\"" + testCid + "\"}");

        when(httpResponse.statusCode())
                .thenReturn(200);

        String resultCid = client.createRecord(testDid, "app.bsky.feed.post", Map.of());
        assertThat(resultCid).isEqualTo(testCid);
    }

    @Test
    void testAuthentication() throws Exception {
        String testHandle = "test.handle";
        String testPassword = "test-password";

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(httpResponse);

        when(httpResponse.body())
                .thenReturn("{\"accessJwt\":\"new-token\",\"refreshJwt\":\"refresh-token\"}");

        when(httpResponse.statusCode())
                .thenReturn(200);

        Map<String, String> authResult = client.authenticate(testHandle, testPassword);
        assertThat(authResult).containsKeys("accessJwt", "refreshJwt");
    }
}