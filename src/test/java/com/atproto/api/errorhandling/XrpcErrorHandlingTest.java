package com.atproto.api.errorhandling;

import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.assertj.core.api.Assertions;

import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

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
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(404);
               Assertions.assertThat(e.getCode()).isEqualTo("not_found");
               Assertions.assertThat(e.getMessage()).isEqualTo("Resource not found");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleNetworkError() throws Exception {
        // Given
        RuntimeException networkError = new RuntimeException("Network error");
        when(xrpcClient.sendRequest(any())).thenThrow(networkError);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.sendRequest();
        }).isInstanceOf(XrpcException.class)
           .hasMessageContaining("Network error")
           .satisfies(e -> Assertions.assertThat(e.isNetworkError()).isTrue());

        verify(xrpcClient).sendRequest(any());
    }

    @Test
    public void shouldHandleTimeoutError() throws Exception {
        // Given
        TimeoutException timeoutError = new TimeoutException("Request timed out");
        when(xrpcClient.sendRequest(any())).thenThrow(timeoutError);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.sendRequest();
        }).isInstanceOf(XrpcException.class)
           .hasMessageContaining("timeout")
           .satisfies(e -> Assertions.assertThat(e.isTimeout()).isTrue());

        verify(xrpcClient).sendRequest(any());
    }

    @Test
    public void shouldHandleValidationError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"invalid_request\",\"message\":\"Invalid parameter: value must be a string\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(400);
               Assertions.assertThat(e.getCode()).isEqualTo("invalid_request");
               Assertions.assertThat(e.getMessage()).contains("Invalid parameter");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleMultipleErrors() throws Exception {
        // Given
        String errorJson = "{\"error\":\"validation_error\",\"message\":\"Multiple validation errors\",\"details\":[\"Field1 is required\",\"Field2 must be numeric\"]}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(400);
               Assertions.assertThat(e.getCode()).isEqualTo("validation_error");
               Assertions.assertThat(e.getMessage()).contains("Multiple validation errors");
               Assertions.assertThat(e.getDetails()).containsExactly("Field1 is required", "Field2 must be numeric");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleRateLimiting() throws Exception {
        // Given
        String errorJson = "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests, please try again later\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(429);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(429);
               Assertions.assertThat(e.getCode()).isEqualTo("rate_limit_exceeded");
               Assertions.assertThat(e.getMessage()).contains("Too many requests");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleAuthenticationError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"unauthorized\",\"message\":\"Invalid credentials\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(401);
               Assertions.assertThat(e.getCode()).isEqualTo("unauthorized");
               Assertions.assertThat(e.getMessage()).contains("Invalid credentials");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleServerError() throws Exception {
        // Given
        String errorJson = "{\"error\":\"server_error\",\"message\":\"Internal server error\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(errorJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .satisfies(e -> {
               Assertions.assertThat(e.getStatusCode()).isEqualTo(500);
               Assertions.assertThat(e.getCode()).isEqualTo("server_error");
               Assertions.assertThat(e.getMessage()).contains("Internal server error");
           });

        verify(response).statusCode();
        verify(response).body();
    }

    @Test
    public void shouldHandleMalformedResponse() throws Exception {
        // Given
        String malformedJson = "{\"error\":\"invalid_json\",\"message\":Invalid JSON\"}";
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(malformedJson);

        // When & Then
        XrpcException exception = Assertions.assertThatThrownBy(() -> {
            errorHandling.handleResponse(response);
        }).isInstanceOf(XrpcException.class)
           .hasMessageContaining("Malformed JSON response");

        verify(response).statusCode();
        verify(response).body();
    }
}
