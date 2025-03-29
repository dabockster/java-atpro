package com.atproto.security;

import com.atproto.api.xrpc.XrpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersTest {

    @InjectMocks
    private SecurityHeadersFilter securityHeadersFilter;

    private XrpcResponse response;

    @BeforeEach
    void setUp() {
        response = new XrpcResponse();
    }

    @Test
    void testAllSecurityHeadersPresent() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        assertThat(response.getHeaders().get("Strict-Transport-Security")).isNotNull();
        assertThat(response.getHeaders().get("X-Content-Type-Options")).isNotNull();
        assertThat(response.getHeaders().get("X-Frame-Options")).isNotNull();
        assertThat(response.getHeaders().get("X-XSS-Protection")).isNotNull();
        assertThat(response.getHeaders().get("Content-Security-Policy")).isNotNull();
        assertThat(response.getHeaders().get("Referrer-Policy")).isNotNull();
        assertThat(response.getHeaders().get("X-Permitted-Cross-Domain-Policies")).isNotNull();
        assertThat(response.getHeaders().get("X-Download-Options")).isNotNull();
        assertThat(response.getHeaders().get("X-Content-Type-Options")).isNotNull();
    }

    @Test
    void testContentSecurityPolicy() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String csp = response.getHeaders().get("Content-Security-Policy").get(0);
        assertThat(csp).contains("default-src 'self'")
                       .contains("script-src 'self' 'unsafe-inline'")
                       .contains("style-src 'self' 'unsafe-inline'")
                       .contains("img-src 'self' data:")
                       .contains("connect-src 'self'")
                       .contains("font-src 'self'");
    }

    @Test
    void testStrictTransportSecurity() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String hsts = response.getHeaders().get("Strict-Transport-Security").get(0);
        assertThat(hsts).contains("max-age=31536000")
                       .contains("includeSubDomains")
                       .contains("preload");
    }

    @Test
    void testReferrerPolicy() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String referrer = response.getHeaders().get("Referrer-Policy").get(0);
        assertThat(referrer).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    void testXFrameOptions() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String frame = response.getHeaders().get("X-Frame-Options").get(0);
        assertThat(frame).isEqualTo("DENY");
    }

    @Test
    void testXContentTypeOptions() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String contentType = response.getHeaders().get("X-Content-Type-Options").get(0);
        assertThat(contentType).isEqualTo("nosniff");
    }

    @Test
    void testXXSSProtection() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String xss = response.getHeaders().get("X-XSS-Protection").get(0);
        assertThat(xss).isEqualTo("1; mode=block");
    }

    @ParameterizedTest
    @ValueSource(strings = {"X-Permitted-Cross-Domain-Policies", "X-Download-Options"})
    void testAdditionalSecurityHeaders(String headerName) {
        securityHeadersFilter.addSecurityHeaders(response);
        
        assertThat(response.getHeaders().get(headerName)).isNotNull();
    }

    @Test
    void testSecurityHeadersOrder() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        List<String> headers = response.getHeaders().keySet().stream()
            .filter(h -> h.startsWith("X-"))
            .toList();
            
        assertThat(headers).containsExactly(
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection",
            "X-Permitted-Cross-Domain-Policies",
            "X-Download-Options"
        );
    }

    @Test
    void testHeaderValuesAreImmutable() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        List<String> headers = response.getHeaders().get("Content-Security-Policy");
        assertThrows(UnsupportedOperationException.class, () -> {
            headers.add("test");
        });
    }

    @Test
    void testHeadersAreAddedOnlyOnce() {
        securityHeadersFilter.addSecurityHeaders(response);
        securityHeadersFilter.addSecurityHeaders(response);
        
        assertEquals(1, response.getHeaders().get("Strict-Transport-Security").size());
    }

    @Test
    void testHeadersAreCaseInsensitive() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String hsts = response.getHeaders().get("strict-transport-security").get(0);
        assertNotNull(hsts);
    }
}
