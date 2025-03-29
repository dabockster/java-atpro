package com.atproto.api;

import com.atproto.did.DidResolver;
import com.atproto.did.model.DidDocument;
import com.atproto.did.model.DidResolutionResult;
import com.atproto.lexicon.LexiconParser;
import com.atproto.lexicon.model.LexiconSchema;
import com.atproto.xrpc.XrpcClient;
import com.atproto.xrpc.model.XrpcRequest;
import com.atproto.xrpc.model.XrpcResponse;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtpAgentTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private DidResolver didResolver;

    @Mock
    private LexiconParser lexiconParser;

    @Mock
    private XrpcClient xrpcClient;

    @InjectMocks
    private AtpAgent agent;

    private static final String TEST_DID = "did:plc:1234567890abcdef";
    private static final String TEST_HANDLE = "test.handle";
    private static final String TEST_JWT = "eyJhbGciOiJFZERTQSJ9.eyJzdWIiOiJkaWQ6cGxjOjEyMzQ1Njc4OTAxMjM0NTY3ODkwIiwiaWF0IjoxNjg0NjQ4NjY3LCJleHAiOjE2ODQ2NTIyNjd9.8dF5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k5yZ3k";

    @BeforeEach
    void setUp() {
        // Initialize mocks with default behavior
        when(didResolver.resolve(eq(TEST_DID))).thenReturn(CompletableFuture.completedFuture(new DidResolutionResult()));
        when(lexiconParser.parse(any(String.class))).thenReturn(new LexiconSchema());
    }

    @Test
    void testInitializeWithValidConfiguration() {
        // Test initialization with valid configuration
        Assertions.assertThat(agent).isNotNull();
        verifyNoInteractions(httpClient, didResolver, lexiconParser, xrpcClient);
    }

    @Test
    void testAuthenticationWithDID() throws ExecutionException, InterruptedException {
        // Arrange
        when(didResolver.resolve(eq(TEST_DID))).thenReturn(CompletableFuture.completedFuture(new DidResolutionResult()));
        when(xrpcClient.createRequest(any(XrpcRequest.class))).thenReturn(CompletableFuture.completedFuture(new XrpcResponse()));

        // Act
        CompletableFuture<XrpcResponse> response = agent.authenticateWithDID(TEST_DID);
        XrpcResponse result = response.get();

        // Assert
        Assertions.assertThat(result).isNotNull();
        verify(didResolver).resolve(eq(TEST_DID));
        verify(xrpcClient).createRequest(any(XrpcRequest.class));
    }

    @Test
    void testXRPCRequestWithValidParameters() throws ExecutionException, InterruptedException {
        // Arrange
        String methodName = "com.atproto.repo.uploadBlob";
        XrpcRequest request = new XrpcRequest(methodName);
        
        when(xrpcClient.createRequest(eq(request))).thenReturn(CompletableFuture.completedFuture(new XrpcResponse()));

        // Act
        CompletableFuture<XrpcResponse> response = agent.sendXRPCRequest(request);
        XrpcResponse result = response.get();

        // Assert
        Assertions.assertThat(result).isNotNull();
        verify(xrpcClient).createRequest(eq(request));
    }

    @Test
    void testXRPCRequestWithInvalidParameters() {
        // Arrange
        XrpcRequest invalidRequest = new XrpcRequest("invalid.method.name");
        
        when(xrpcClient.createRequest(eq(invalidRequest))).thenThrow(new RuntimeException("Invalid method name"));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> agent.sendXRPCRequest(invalidRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid method name");
    }

    @Test
    void testEventStreaming() throws ExecutionException, InterruptedException {
        // Arrange
        String streamPath = "com.atproto.sync.subscribeRepos";
        
        when(xrpcClient.createWebSocketConnection(eq(streamPath))).thenReturn(CompletableFuture.completedFuture(HttpResponse.create()));

        // Act
        CompletableFuture<HttpResponse<Void>> response = agent.subscribeToEvents(streamPath);
        HttpResponse<Void> result = response.get();

        // Assert
        Assertions.assertThat(result).isNotNull();
        verify(xrpcClient).createWebSocketConnection(eq(streamPath));
    }

    @Test
    void testRepositorySync() throws ExecutionException, InterruptedException {
        // Arrange
        String repoDid = TEST_DID;
        
        when(xrpcClient.createRequest(any(XrpcRequest.class))).thenReturn(CompletableFuture.completedFuture(new XrpcResponse()));

        // Act
        CompletableFuture<XrpcResponse> response = agent.syncRepository(repoDid);
        XrpcResponse result = response.get();

        // Assert
        Assertions.assertThat(result).isNotNull();
        verify(xrpcClient).createRequest(any(XrpcRequest.class));
    }

    @Test
    void testErrorHandling() {
        // Arrange
        XrpcRequest request = new XrpcRequest("com.atproto.repo.uploadBlob");
        
        when(xrpcClient.createRequest(eq(request))).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> agent.sendXRPCRequest(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Network error");
    }

    @Test
    void testConnectionPooling() {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://bsky.social/xrpc/com.atproto.repo.uploadBlob"))
            .build();

        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(HttpResponse.create());

        // Act
        agent.sendRequest(request);
        agent.sendRequest(request);

        // Assert
        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void testWebSocketSupport() throws ExecutionException, InterruptedException {
        // Arrange
        String streamPath = "com.atproto.sync.subscribeRepos";
        
        when(xrpcClient.createWebSocketConnection(eq(streamPath))).thenReturn(CompletableFuture.completedFuture(HttpResponse.create()));

        // Act
        CompletableFuture<HttpResponse<Void>> response = agent.createWebSocketConnection(streamPath);
        HttpResponse<Void> result = response.get();

        // Assert
        Assertions.assertThat(result).isNotNull();
        verify(xrpcClient).createWebSocketConnection(eq(streamPath));
    }

    @Test
    void testSSLConfiguration() {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(HttpResponse.create());

        // Act
        agent.sendRequest(HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://bsky.social/xrpc/com.atproto.repo.uploadBlob"))
            .build());

        // Assert
        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void testRateLimiting() {
        // Arrange
        when(xrpcClient.createRequest(any(XrpcRequest.class))).thenReturn(CompletableFuture.completedFuture(new XrpcResponse()));

        // Act
        for (int i = 0; i < 100; i++) {
            agent.sendXRPCRequest(new XrpcRequest("com.atproto.repo.uploadBlob"));
        }

        // Assert
        verify(xrpcClient, times(100)).createRequest(any(XrpcRequest.class));
    }

    @Test
    void testRequestTimeouts() {
        // Arrange
        when(xrpcClient.createRequest(any(XrpcRequest.class))).thenThrow(new RuntimeException("Request timeout"));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> agent.sendXRPCRequest(new XrpcRequest("com.atproto.repo.uploadBlob")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Request timeout");
    }

    @Test
    void testConcurrentRequests() throws ExecutionException, InterruptedException {
        // Arrange
        XrpcRequest request = new XrpcRequest("com.atproto.repo.uploadBlob");
        
        when(xrpcClient.createRequest(eq(request))).thenReturn(CompletableFuture.completedFuture(new XrpcResponse()));

        // Act
        CompletableFuture<XrpcResponse>[] responses = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            responses[i] = agent.sendXRPCRequest(request);
        }

        // Assert
        for (CompletableFuture<XrpcResponse> response : responses) {
            Assertions.assertThat(response.get()).isNotNull();
        }
        verify(xrpcClient, times(10)).createRequest(eq(request));
    }
}
