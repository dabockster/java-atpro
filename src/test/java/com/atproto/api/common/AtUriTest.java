package com.atproto.api.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.assertThrows;
import org.junit.jupiter.api.function.Executable;
import org.assertj.core.api.Assertions;
import org.apache.commons.lang3.StringUtils;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class AtUriTest {

    @Test
    @DisplayName("Valid AT URIs should be parsed correctly")
    public void testValidAtUris() {
        String[] validUris = {
            "at://did:abc:123/io.nsid.someFunc/record-key",
            "at://e.com",
            "at://did:abc:123/io.NsId.someFunc/record-KEY", // Should be normalized
            "at://E.com", // Should be normalized
            "at://did:plc:1234567890abcdef/io.example.collection/record-key"
        };

        for (String uri : validUris) {
            AtUri atUri = AtUri.parse(uri);
            assertNotNull(atUri);
            
            // Test normalization
            if (uri.contains("E.com") || uri.contains("NsId")) {
                String normalized = atUri.normalize().toString();
                assertFalse(normalized.contains("E.com"));
                assertFalse(normalized.contains("NsId"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", // Empty string
        "at://", // Missing authority
        "at://e.com/", // Trailing slash
        "at://e.com//", // Double slash
        "at://@handle.example.com", // Invalid handle format
        "at://did:web:example.com:8080", // Invalid DID format
        "at://did:abc:123/invalid-path/record-key", // Invalid path format
        "at://did:abc:123/io.nsid.someFunc/record/key", // Invalid record key format
        "at://did:abc:123/io.nsid.someFunc?query=param", // Invalid query parameter
        "at://did:abc:123/io.nsid.someFunc#fragment" // Invalid fragment
    })
    @DisplayName("Invalid AT URIs should throw exceptions")
    public void testInvalidAtUris(String invalidUri) {
        assertThrows(IllegalArgumentException.class, () -> AtUri.parse(invalidUri));
    }

    @ParameterizedTest
    @CsvSource({
        "at://did:abc:123/io.nsid.someFunc/record-key, did:abc:123, io.nsid.someFunc, record-key",
        "at://did:abc:123/io.nsid.someFunc, did:abc:123, io.nsid.someFunc,",
        "at://did:abc:123, did:abc:123, ,"
    })
    @DisplayName("AT URI parts should be extracted correctly")
    public void testPartsExtraction(String uri, String authority, String collection, String recordKey) {
        AtUri atUri = AtUri.parse(uri);
        assertEquals(authority, atUri.getAuthority());
        assertEquals(collection, atUri.getCollection());
        assertEquals(recordKey, atUri.getRecordKey());
        assertEquals(StringUtils.isEmpty(recordKey) ? collection : collection + "/" + recordKey, atUri.getPath());
    }

    @Test
    @DisplayName("AT URI normalization should work correctly")
    public void testNormalization() {
        AtUri atUri = AtUri.parse("at://did:abc:123/io.NsId.someFunc/record-KEY");
        AtUri normalized = atUri.normalize();
        assertEquals("at://did:abc:123/io.nsid.someFunc/record-KEY", normalized.toString());

        atUri = AtUri.parse("at://E.com");
        normalized = atUri.normalize();
        assertEquals("at://e.com", normalized.toString());
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    @DisplayName("AT URI should handle edge cases without crashing")
    public void testNoPanic(String uri) {
        AtUri atUri = new AtUri(uri);
        assertNotNull(atUri.getAuthority());
        assertNotNull(atUri.getCollection());
        assertNotNull(atUri.getRecordKey());
        assertNotNull(atUri.normalize());
        assertNotNull(atUri.toString());
        assertNotNull(atUri.getPath());
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
            Arguments.of(""), // Empty string
            Arguments.of("."), // Single dot
            Arguments.of("at://"), // Scheme only
            Arguments.of("at:///"), // Scheme with slash
            Arguments.of("at://e.com"), // Valid but minimal
            Arguments.of("at://e.com/"), // Valid with trailing slash
            Arguments.of("at://e.com//") // Valid with double slash
        );
    }

    @Test
    @DisplayName("AT URI should enforce maximum length")
    public void testUriLength() {
        // Test maximum length (8KB)
        StringBuilder longUri = new StringBuilder("at://did:abc:123/");
        while (longUri.length() < 8192) {
            longUri.append("a");
        }
        
        assertThrows(IllegalArgumentException.class, () -> AtUri.parse(longUri.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "at://did:abc:123/io.nsid.someFunc/record-key",
        "at://e.com",
        "at://did:abc:123/io.NsId.someFunc/record-KEY"
    })
    @DisplayName("AT URI should maintain equality with normalized form")
    public void testEquality(String uri) {
        AtUri original = AtUri.parse(uri);
        AtUri normalized = original.normalize();
        
        assertEquals(original, normalized);
        assertEquals(original.hashCode(), normalized.hashCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "at://did:abc:123/io.nsid.someFunc/record-key",
        "at://did:abc:123/io.nsid.someFunc",
        "at://did:abc:123"
    })
    @DisplayName("AT URI should match regex pattern")
    public void testRegexPattern(String uri) {
        Pattern pattern = Pattern.compile("^at://([a-zA-Z0-9.-]+|did:[a-zA-Z0-9.-]+:[a-zA-Z0-9.-]+)/?([a-zA-Z0-9.-]+/)?([a-zA-Z0-9.-]+)?$",
                Pattern.CASE_INSENSITIVE);
        assertTrue(pattern.matcher(uri).matches());
    }
}
