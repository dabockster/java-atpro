package com.atproto.security;

import com.atproto.api.xrpc.XrpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
        
        assertNotNull(response.getHeaders().get("Strict-Transport-Security"));
        assertNotNull(response.getHeaders().get("X-Content-Type-Options"));
        assertNotNull(response.getHeaders().get("X-Frame-Options"));
        assertNotNull(response.getHeaders().get("X-XSS-Protection"));
        assertNotNull(response.getHeaders().get("Content-Security-Policy"));
        assertNotNull(response.getHeaders().get("Referrer-Policy"));
        assertNotNull(response.getHeaders().get("X-Permitted-Cross-Domain-Policies"));
        assertNotNull(response.getHeaders().get("X-Download-Options"));
        assertNotNull(response.getHeaders().get("X-Content-Type-Options"));
    }

    @Test
    void testContentSecurityPolicy() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String csp = response.getHeaders().get("Content-Security-Policy").get(0);
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline'"));
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"));
        assertTrue(csp.contains("img-src 'self' data:"));
        assertTrue(csp.contains("connect-src 'self'"));
        assertTrue(csp.contains("font-src 'self'"));
    }

    @Test
    void testStrictTransportSecurity() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String hsts = response.getHeaders().get("Strict-Transport-Security").get(0);
        assertTrue(hsts.contains("max-age=31536000"));
        assertTrue(hsts.contains("includeSubDomains"));
        assertTrue(hsts.contains("preload"));
    }

    @Test
    void testReferrerPolicy() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String referrer = response.getHeaders().get("Referrer-Policy").get(0);
        assertEquals("strict-origin-when-cross-origin", referrer);
    }

    @Test
    void testXFrameOptions() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String frame = response.getHeaders().get("X-Frame-Options").get(0);
        assertEquals("DENY", frame);
    }

    @Test
    void testXContentTypeOptions() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String contentType = response.getHeaders().get("X-Content-Type-Options").get(0);
        assertEquals("nosniff", contentType);
    }

    @Test
    void testXXSSProtection() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String xss = response.getHeaders().get("X-XSS-Protection").get(0);
        assertEquals("1; mode=block", xss);
    }

    @Test
    void testXPermittedCrossDomainPolicies() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String crossDomain = response.getHeaders().get("X-Permitted-Cross-Domain-Policies").get(0);
        assertEquals("none", crossDomain);
    }

    @Test
    void testXDownloadOptions() {
        securityHeadersFilter.addSecurityHeaders(response);
        
        String download = response.getHeaders().get("X-Download-Options").get(0);
        assertEquals("noopen", download);
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
