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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit5.PowerMockExtension;
import org.owasp.dependencycheck.utils.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.*;

import java.util.stream.Stream;
import java.util.regex.Pattern;

@ExtendWith({MockitoExtension.class, PowerMockExtension.class})
@PrepareForTest(AtUri.class)
public class AtUriTest {

    @Mock
    private AtUri mockAtUri;

    @Test
    @DisplayName("Valid AT URIs should be parsed correctly")
    public void testValidAtUris() {
        String[] validUris = {
            "at://did:abc:123/io.nsid.someFunc/record-key",
            "at://e.com",
            "at://did:abc:123/io.NsId.someFunc/record-KEY",
            "at://E.com",
            "at://did:plc:1234567890abcdef/io.example.collection/record-key"
        };

        for (String uri : validUris) {
            AtUri atUri = AtUri.parse(uri);
            assertThat(atUri).isNotNull();
            
            // Test normalization
            if (uri.contains("E.com") || uri.contains("NsId")) {
                String normalized = atUri.normalize().toString();
                assertThat(normalized).doesNotContain("E.com");
                assertThat(normalized).doesNotContain("NsId");
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
        assertThatThrownBy(() -> AtUri.parse(invalidUri))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid AT URI");
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
        assertThat(atUri.getAuthority()).isEqualTo(authority);
        assertThat(atUri.getCollection()).isEqualTo(collection);
        assertThat(atUri.getRecordKey()).isEqualTo(recordKey);
        assertThat(atUri.getPath()).isEqualTo(recordKey.isEmpty() ? collection : collection + "/" + recordKey);
    }

    @Test
    @DisplayName("AT URI normalization should work correctly")
    public void testNormalization() {
        AtUri atUri = AtUri.parse("at://did:abc:123/io.NsId.someFunc/record-KEY");
        AtUri normalized = atUri.normalize();
        assertThat(normalized.toString()).isEqualTo("at://did:abc:123/io.nsid.someFunc/record-KEY");

        atUri = AtUri.parse("at://E.com");
        normalized = atUri.normalize();
        assertThat(normalized.toString()).isEqualTo("at://e.com");
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    @DisplayName("AT URI should handle edge cases without crashing")
    public void testNoPanic(String uri) {
        mockStatic(AtUri.class);
        when(AtUri.parse(any(String.class))).thenReturn(mockAtUri);
        
        AtUri atUri = new AtUri(uri);
        assertThat(atUri.getAuthority()).isNotNull();
        assertThat(atUri.getCollection()).isNotNull();
        assertThat(atUri.getRecordKey()).isNotNull();
        assertThat(atUri.normalize()).isNotNull();
        assertThat(atUri.toString()).isNotNull();
        assertThat(atUri.getPath()).isNotNull();
        
        verifyStatic(AtUri.class);
        AtUri.parse(uri);
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
            Arguments.of(""), // Empty string
            Arguments.of("at://did:abc:123/io.nsid.someFunc/record-key"), // Valid URI
            Arguments.of("at://e.com") // Valid handle
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
        
        assertThatThrownBy(() -> AtUri.parse(longUri.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URI exceeds maximum length");
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
        
        assertThat(original).isEqualTo(normalized);
        assertThat(original.hashCode()).isEqualTo(normalized.hashCode());
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
        assertThat(pattern.matcher(uri).matches()).isTrue();
    }

    @Test
    @DisplayName("AT URI should handle malformed input gracefully")
    public void testMalformedInput() {
        // Test null input
        assertThatThrownBy(() -> AtUri.parse(null))
            .isInstanceOf(NullPointerException.class);

        // Test whitespace-only input
        assertThatThrownBy(() -> AtUri.parse(" "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AT URI should handle special characters correctly")
    public void testSpecialCharacters() {
        String uri = "at://did:abc:123/io.nsid.someFunc/record-key_with-special_chars_123";
        AtUri atUri = AtUri.parse(uri);
        assertThat(atUri.getRecordKey()).isEqualTo("record-key_with-special_chars_123");
    }
}
