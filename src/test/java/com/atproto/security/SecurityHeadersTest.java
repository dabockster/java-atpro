package com.atproto.security;

import com.atproto.api.xrpc.XrpcResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersTest {

    @InjectMocks
    private SecurityHeadersFilter securityHeadersFilter;

    @Test
    void testSecurityHeaders() {
        XrpcResponse response = new XrpcResponse();
        securityHeadersFilter.addSecurityHeaders(response);

        assertNotNull(response.getHeaders().get("Strict-Transport-Security"));
        assertNotNull(response.getHeaders().get("X-Content-Type-Options"));
        assertNotNull(response.getHeaders().get("X-Frame-Options"));
        assertNotNull(response.getHeaders().get("X-XSS-Protection"));
        assertNotNull(response.getHeaders().get("Content-Security-Policy"));
        assertNotNull(response.getHeaders().get("Referrer-Policy"));
    }

    @Test
    void testCSPHeader() {
        XrpcResponse response = new XrpcResponse();
        securityHeadersFilter.addSecurityHeaders(response);

        String csp = response.getHeaders().get("Content-Security-Policy").get(0);
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline'"));
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"));
    }

    @Test
    void testHSTSHeader() {
        XrpcResponse response = new XrpcResponse();
        securityHeadersFilter.addSecurityHeaders(response);

        String hsts = response.getHeaders().get("Strict-Transport-Security").get(0);
        assertTrue(hsts.contains("max-age=31536000"));
        assertTrue(hsts.contains("includeSubDomains"));
    }

    @Test
    void testReferrerPolicy() {
        XrpcResponse response = new XrpcResponse();
        securityHeadersFilter.addSecurityHeaders(response);

        String referrer = response.getHeaders().get("Referrer-Policy").get(0);
        assertEquals("strict-origin-when-cross-origin", referrer);
    }
}
