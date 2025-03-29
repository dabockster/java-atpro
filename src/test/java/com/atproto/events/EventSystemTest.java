package com.atproto.events;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcRequest;
import com.atproto.api.xrpc.XrpcResponse;
import com.atproto.did.DidResolver;
import com.atproto.lexicon.LexiconParser;
import com.atproto.repository.RepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EventSystemTest {

    @Mock
    private XrpcClient xrpcClient;

    @Mock
    private DidResolver didResolver;

    @Mock
    private LexiconParser lexiconParser;

    @Mock
    private RepositoryManager repositoryManager;

    private EventSystem eventSystem;

    @BeforeEach
    public void setUp() {
        eventSystem = new EventSystem(xrpcClient, didResolver, lexiconParser, repositoryManager);
    }

    @Test
    public void testEventStreamingSuccess() throws ExecutionException, InterruptedException {
        // Given
        String did = "did:plc:example";
        CompletableFuture<XrpcResponse> mockResponse = CompletableFuture.completedFuture(
            new XrpcResponse("{"data": {"cid": "bafy..."}}")
        );
        when(xrpcClient.send(any(XrpcRequest.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<XrpcResponse> result = eventSystem.subscribeToEvents(did);

        // Then
        assertNotNull(result.get());
        verify(xrpcClient).send(any(XrpcRequest.class));
    }

    @Test
    public void testSubscriptionManagement() {
        // Given
        String did = "did:plc:example";
        String subscriptionId = "sub123";

        // When
        eventSystem.subscribe(did, subscriptionId);
        boolean unsubscribed = eventSystem.unsubscribe(subscriptionId);

        // Then
        assertTrue(unsubscribed);
        assertTrue(eventSystem.getSubscriptions().isEmpty());
    }

    @Test
    public void testEventValidation() {
        // Given
        String validEvent = "{"data": {"cid": "bafy..."}}";
        String invalidEvent = "{"data": {"invalid": "field"}}";

        // When
        boolean isValid = eventSystem.validateEvent(validEvent);
        boolean isInvalid = eventSystem.validateEvent(invalidEvent);

        // Then
        assertTrue(isValid);
        assertFalse(isInvalid);
    }

    @Test
    public void testEventDeliveryGuarantees() throws ExecutionException, InterruptedException {
        // Given
        String did = "did:plc:example";
        String cid = "bafy...";
        CompletableFuture<XrpcResponse> mockResponse = CompletableFuture.completedFuture(
            new XrpcResponse("{"data": {"cid": "" + cid + ""}}")
        );
        when(xrpcClient.send(any(XrpcRequest.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<XrpcResponse> result = eventSystem.publishEvent(did, cid);

        // Then
        assertNotNull(result.get());
        verify(xrpcClient).send(any(XrpcRequest.class));
    }

    @Test
    public void testErrorHandling() {
        // Given
        String did = "did:plc:example";
        CompletableFuture<XrpcResponse> mockResponse = CompletableFuture.failedFuture(
            new RuntimeException("Test error")
        );
        when(xrpcClient.send(any(XrpcRequest.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<XrpcResponse> result = eventSystem.subscribeToEvents(did);

        // Then
        assertThrows(ExecutionException.class, () -> result.get());
        verify(xrpcClient).send(any(XrpcRequest.class));
    }

    @Test
    public void testTimeoutHandling() throws ExecutionException, InterruptedException {
        // Given
        String did = "did:plc:example";
        CompletableFuture<XrpcResponse> mockResponse = new CompletableFuture<>();
        when(xrpcClient.send(any(XrpcRequest.class))).thenReturn(mockResponse);

        // When
        CompletableFuture<XrpcResponse> result = eventSystem.subscribeToEvents(did);

        // Then
        assertThrows(TimeoutException.class, () -> result.get(1, java.util.concurrent.TimeUnit.SECONDS));
        verify(xrpcClient).send(any(XrpcRequest.class));
    }

    @Test
    public void testEventBatching() {
        // Given
        int batchSize = 10;
        String did = "did:plc:example";
        when(xrpcClient.getBatchSize()).thenReturn(batchSize);

        // When
        int actualBatchSize = eventSystem.getBatchSize(did);

        // Then
        assertEquals(batchSize, actualBatchSize);
    }

    @Test
    public void testEventRecovery() {
        // Given
        String did = "did:plc:example";
        String lastCid = "bafy...";
        when(repositoryManager.getLastCid(did)).thenReturn(lastCid);

        // When
        String recoveredCid = eventSystem.recoverLastEvent(did);

        // Then
        assertEquals(lastCid, recoveredCid);
    }

    @Test
    public void testEventPersistence() {
        // Given
        String did = "did:plc:example";
        String cid = "bafy...";
        when(repositoryManager.persistEvent(did, cid)).thenReturn(true);

        // When
        boolean persisted = eventSystem.persistEvent(did, cid);

        // Then
        assertTrue(persisted);
    }

    @Test
    public void testEventSubscriptionValidation() {
        // Given
        String invalidDid = "invalid:did";
        String validDid = "did:plc:example";

        // When
        boolean invalid = eventSystem.validateSubscription(invalidDid);
        boolean valid = eventSystem.validateSubscription(validDid);

        // Then
        assertFalse(invalid);
        assertTrue(valid);
    }
}
