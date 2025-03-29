package com.atproto.api.xrpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(PowerMockRunnerDelegate.class)
@PrepareForTest({XrpcRequest.class})
public class XrpcRequestTest {

    private static final String TEST_METHOD = "com.atproto.sync.getLatestCommit";
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_HANDLE = "example.com";

    private XrpcRequest request;

    @BeforeEach
    public void setUp() {
        Map<String, Object> params = new HashMap<>(Map.of("handle", TEST_HANDLE));
        request = new XrpcRequest(TEST_METHOD, params, null, null, null, null);
    }

    @Test
    public void testRequestCreation() {
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo(TEST_METHOD);
        assertThat(request.getParams()).isEqualTo(new HashMap<>(Map.of("handle", TEST_HANDLE)));
    }

    @Test
    public void testRequestWithAuth() {
        Map<String, Object> params = new HashMap<>(Map.of("handle", TEST_HANDLE));
        XrpcRequest authRequest = new XrpcRequest(TEST_METHOD, params, TEST_TOKEN, null, null, null);

        assertThat(authRequest.getAuth()).isEqualTo(TEST_TOKEN);
    }

    @Test
    public void testRequestSerialization() throws IOException {
        String serialized = request.serialize();
        assertThat(serialized).contains(TEST_METHOD);
        assertThat(serialized).contains(TEST_HANDLE);
    }

    @Test
    public void testRequestWithMultipleParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("handle", TEST_HANDLE);
        params.put("limit", 10);
        params.put("cursor", "abc123");

        XrpcRequest multiParamRequest = new XrpcRequest(TEST_METHOD, params, null, null, null, null);
        assertEquals(params, multiParamRequest.getParams());
    }

    @Test
    public void testRequestWithEmptyParams() {
        XrpcRequest emptyParamsRequest = new XrpcRequest(TEST_METHOD, null, null, null, null, null);
        assertNotNull(emptyParamsRequest);
        assertEquals(TEST_METHOD, emptyParamsRequest.getMethod());
        assertTrue(emptyParamsRequest.getParams().isEmpty());
    }

    @Test
    public void testRequestWithNullParams() {
        XrpcRequest nullParamsRequest = new XrpcRequest(TEST_METHOD, null, null, null, null, null);
        assertNotNull(nullParamsRequest);
        assertTrue(nullParamsRequest.getParams().isEmpty());
    }

    @Test
    public void testRequestDeserialization() throws Exception {
        String json = "{\"method\":\"com.atproto.identity.resolveHandle\",\"params\":{\"handle\":\"example.com\"}}";
        XrpcRequest deserialized = XrpcRequest.deserialize(json);

        assertNotNull(deserialized);
        assertEquals("com.atproto.identity.resolveHandle", deserialized.getMethod());
        assertEquals(new HashMap<>(Map.of("handle", "example.com")), deserialized.getParams());
    }

    @Test
    public void testRequestValidation() {
        Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());

        XrpcRequest invalidRequest = new XrpcRequest("invalid.method", null, null, null, null, null);
        errors = invalidRequest.validate();
        assertFalse(errors.isEmpty());
        assertTrue(errors.containsKey("method"));
    }

    @Test
    public void testRequestEqualsAndHashCode() {
        Map<String, Object> params = new HashMap<>(Map.of("handle", TEST_HANDLE));
        XrpcRequest request1 = new XrpcRequest(TEST_METHOD, params, null, null, null, null);
        XrpcRequest request2 = new XrpcRequest(TEST_METHOD, params, null, null, null, null);

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());

        XrpcRequest differentRequest = new XrpcRequest("different.method", params, null, null, null, null);
        assertNotEquals(request1, differentRequest);
    }
}
