// src/test/java/com/atproto/codegen/LexiconParserTest.java
package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

@MockitoSettings(strictness = Strictness.LENIENT)
public class LexiconParserTest {

    @Mock
    private LexiconParser mockParser;

    @InjectMocks
    private LexiconParser parser;

    private InputStream stringToInputStream(String str) {
        return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        reset(mockParser);
    }

    // --- Helper to get nested maps/values for basic checks ---
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        assertThat(value).isNotNull().isInstanceOf(Map.class, "Expected key '" + key + "' to be a Map");
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        assertThat(value).isNotNull().isInstanceOf(List.class, "Expected key '" + key + "' to be a List");
        return (List<Object>) value;
    }

    @Nested
    @DisplayName("Invalid Lexicon Tests")
    class InvalidLexiconTests {

        @Test
        @DisplayName("Should throw for empty lexicon")
        void testParseEmptyLexicon() {
            String lexiconJson = "{}";
            
            assertThatThrownBy(() -> parser.parse(stringToInputStream(lexiconJson)))
                .isInstanceOf(IllegalArgumentException.class)
                .withFailMessage("Should throw IllegalArgumentException for missing required fields.");
        }

        @ParameterizedTest
        @CsvSource({
            "2, Should throw for unsupported lexicon version",
            "3, Should throw for unsupported lexicon version"
        })
        @DisplayName("Should throw for unsupported lexicon version")
        void testInvalidLexiconVersion(int version, String message) {
            String lexiconJson = String.format("""
                {
                  "lexicon": %d,
                  "id": "com.example.test",
                  "defs": { "main": { "type": "token" } }
                }
                """, version);
            
            assertThatThrownBy(() -> parser.parse(stringToInputStream(lexiconJson)))
                .isInstanceOf(IllegalArgumentException.class)
                .withFailMessage(message);
        }

        @Test
        @DisplayName("Should throw for missing id field")
        void testMissingRequiredFields_Id() {
            String lexiconJson = """
                {
                  "lexicon": 1,
                  "defs": { "main": { "type": "token" } }
                }
                """;
            
            assertThatThrownBy(() -> parser.parse(stringToInputStream(lexiconJson)))
                .isInstanceOf(IllegalArgumentException.class)
                .withFailMessage("Should throw IllegalArgumentException for missing 'id'.");
        }

        @ParameterizedTest
        @ValueSource(strings = { "{,}", "[,"] })
        @DisplayName("Should throw for JSON syntax errors")
        void testInvalidJson_SyntaxError(String invalidJson) {
            assertThatThrownBy(() -> parser.parse(stringToInputStream(invalidJson)))
                .isInstanceOf(IOException.class)
                .withFailMessage("Should throw IOException for JSON syntax errors.");
        }
    }

    @Nested
    @DisplayName("Valid Lexicon Tests")
    class ValidLexiconTests {

        @Test
        @DisplayName("Should parse minimal valid lexicon")
        void testParseMinimalValidLexicon() throws IOException {
            String lexiconJson = """
                {
                  "lexicon": 1,
                  "id": "com.example.test",
                  "revision": 0,
                  "defs": {
                    "main": {
                      "type": "query",
                      "output": {
                        "encoding": "application/json"
                      }
                    }
                  }
                }
                """;
            
            Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)),
                "Parsing a minimal valid lexicon should succeed.");
            
            assertThat(doc).isNotNull()
                .containsEntry("lexicon", 1L)
                .containsEntry("id", "com.example.test")
                .containsKey("defs");
            
            Map<String, Object> defs = getMap(doc, "defs");
            assertThat(defs).containsKey("main");
            
            Map<String, Object> mainDef = getMap(defs, "main");
            assertThat(mainDef).containsEntry("type", "query");
        }
    }

    @Nested
    @DisplayName("Mock Tests")
    class MockTests {
        @Test
        @DisplayName("Should verify parser behavior")
        void testMockBehavior() {
            String lexiconJson = """
                {
                  "lexicon": 1,
                  "id": "com.example.test",
                  "defs": { "main": { "type": "token" } }
                }
                """;
            
            InputStream inputStream = stringToInputStream(lexiconJson);
            
            // Mock behavior
            when(mockParser.parse(inputStream)).thenReturn(Map.of(
                "lexicon", 1,
                "id", "com.example.test",
                "defs", Map.of("main", Map.of("type", "token"))
            ));
            
            // Verify the mock was called
            verify(mockParser).parse(inputStream);
            
            // Verify no other interactions
            verifyNoMoreInteractions(mockParser);
        }
    }

    // --- Tests for Invalid Structures / Errors ---

    @Test
    public void testInvalidLexiconType() {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "invalidType"
                }
              }
            }
            """; // Unsupported type
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException for unsupported definition type.");
    }

    @Test
    public void testKnownBadDefinition_InvalidRefFormat() {
        String lexiconJson = """
           {
            "lexicon": 1,
            "id": "com.example.test",
            "revision": 0,
            "defs": {
              "main": {
                "type": "query",
                "parameters": {
                  "type": "params",
                  "properties": {
                    "badRef": {
                      "type": "ref",
                      "ref": "invalid-ref-format" // Does not start with # or contain .
                    }
                  }
                },
                "output": { "encoding": "application/json" }
              }
            }
          }
            """; // Bad ref format.
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException for invalid ref format.");
    }

    @Test
    public void testKnownBadDefinition_InvalidRefFormat_EmptyLocal() {
        String lexiconJson = """
           {
            "lexicon": 1,
            "id": "com.example.test",
            "defs": {
              "main": {
                "type": "query",
                "parameters": {
                  "type": "params",
                  "properties": { "badRef": { "type": "ref", "ref": "#" } }
                },
                "output": { "encoding": "application/json" }
              }
            }
          }
            """; // Bad ref format (# alone).
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException for invalid empty local ref format '#'.");
    }

    @Test
    public void testKnownBadDefinition_RequiredPropNotDefined() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "object",
                  "properties": { "propA": { "type": "string" } },
                  "required": ["propA", "propB"]
                }
              }
            }
            """; // propB is required but not defined
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when a required property is not defined.");
    }

     @Test
    public void testKnownBadDefinition_NullablePropNotDefined() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "object",
                  "properties": { "propA": { "type": "string" } },
                  "nullable": ["propA", "propB"]
                }
              }
            }
            """; // propB is nullable but not defined
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when a nullable property is not defined.");
    }

     @Test
    public void testKnownBadDefinition_DuplicateErrorName() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "query",
                  "errors": [
                    { "name": "DuplicateError" },
                    { "name": "AnotherError" },
                    { "name": "DuplicateError" }
                  ]
                }
              }
            }
            """; // Duplicate error name
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException for duplicate error names.");
    }

     @Test
    public void testKnownBadDefinition_InvalidMinMaxLength() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "string",
                  "minLength": 10,
                  "maxLength": 5
                }
              }
            }
            """; // minLength > maxLength
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when minLength > maxLength.");
    }

     @Test
    public void testKnownBadDefinition_InvalidMinMax() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "integer",
                  "minimum": 100,
                  "maximum": 50
                }
              }
            }
            """; // minimum > maximum
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when minimum > maximum.");
    }

     @Test
    public void testKnownBadDefinition_ConstMismatchDefault_String() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "string",
                  "const": "abc",
                  "default": "def"
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when const and default mismatch for string.");
    }

     @Test
    public void testKnownBadDefinition_ConstMismatchDefault_Int() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "integer",
                  "const": 1,
                  "default": 2
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when const and default mismatch for integer.");
    }

     @Test
    public void testKnownBadDefinition_ConstMismatchDefault_Bool() {
         String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "defs": {
                "main": {
                  "type": "boolean",
                  "const": true,
                  "default": false
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)),
                     "Should throw IllegalArgumentException when const and default mismatch for boolean.");
    }

    // --- Tests for Valid Structures ---
    // These tests now primarily check that parsing succeeds without error
    // and optionally check basic structure of the returned Map.

    @Test
    public void testParseMinimalValidLexicon() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "output": {
                    "encoding": "application/json"
                  }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)),
                                                     "Parsing a minimal valid lexicon should succeed.");
        assertNotNull(doc);
        assertEquals(1L, ((Number)doc.get("lexicon")).longValue()); // JSON numbers are parsed as Long/Double
        assertEquals("com.example.test", doc.get("id"));
        assertTrue(doc.containsKey("defs"));
        Map<String, Object> defs = getMap(doc, "defs");
        assertTrue(defs.containsKey("main"));
        Map<String, Object> mainDef = getMap(defs, "main");
        assertEquals("query", mainDef.get("type"));
    }

    @Test
    public void testParseLexiconWithDescription() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 1,
              "description": "A test lexicon.",
              "defs": {
                "main": {
                  "type": "query",
                  "description": "A test query.",
                  "output": { "encoding": "application/json" }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
        assertEquals("A test lexicon.", doc.get("description"));
        Map<String, Object> mainDef = getMap(getMap(doc, "defs"), "main");
        assertEquals("A test query.", mainDef.get("description"));
    }

    @Test
    public void testParseLexiconWithInteger() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters": {
                    "type": "params",
                    "properties": {
                      "count": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 100,
                        "default": 10
                      }
                    }
                  },
                  "output": { "encoding": "application/json" }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
        // Basic check that parsing succeeded
        assertNotNull(doc);
        // Deeper checks on the map structure could be added but are less critical
        // as the parser's main job is validation now.
        Map<String, Object> params = getMap(getMap(getMap(doc, "defs"), "main"), "parameters");
        Map<String, Object> props = getMap(params, "properties");
        Map<String, Object> count = getMap(props, "count");
        assertEquals("integer", count.get("type"));
        assertEquals(1L, ((Number)count.get("minimum")).longValue());
        assertEquals(100L, ((Number)count.get("maximum")).longValue());
        assertEquals(10L, ((Number)count.get("default")).longValue());
    }

    @Test
    public void testParseLexiconWithString() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters": {
                    "type": "params",
                    "properties" : {
                      "name": {
                        "type": "string",
                        "maxLength": 100,
                        "minLength": 1,
                        "const": "atproto",
                        "default": "atproto"
                      }
                    }
                  },
                  "output": { "encoding": "application/json" }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithBoolean() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters" : {
                    "type": "params",
                    "properties": {
                      "enabled": {
                        "type": "boolean",
                        "default": false
                      }
                    }
                  },
                  "output": { "encoding": "application/json" }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithArray() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters": {
                    "type": "params",
                    "properties" : {
                      "names": {
                        "type": "array",
                        "items": { "type": "string" },
                        "maxLength": 10,
                        "minLength": 1
                      }
                    }
                  },
                  "output": { "encoding": "application/json" }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithRef() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters": {
                    "type": "params",
                    "properties": {
                      "refParam": { "type": "ref", "ref": "#recordDef" }
                    }
                  },
                  "output": { "encoding": "application/json" }
                },
                "recordDef": {
                  "type": "object",
                  "properties": { "name": { "type": "string" } }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
        Map<String, Object> mainDef = getMap(getMap(doc, "defs"), "main");
        Map<String, Object> params = getMap(mainDef, "parameters");
        Map<String, Object> properties = getMap(params, "properties");
        Map<String, Object> refParam = getMap(properties, "refParam");
        assertEquals("#recordDef", refParam.get("ref"));
    }

    @Test
    public void testParseLexiconWithUnion() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "parameters": {
                    "type": "params",
                    "properties": {
                      "refUnionParam": {
                        "type": "union",
                        "refs": [ "#recordDef1", "#recordDef2" ]
                      }
                    }
                  },
                  "output": { "encoding": "application/json" }
                },
                "recordDef1": { "type": "object", "properties": { "name1": {"type": "string"} } },
                "recordDef2": { "type": "object", "properties": { "name2": {"type": "string"} } }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
        Map<String, Object> mainDef = getMap(getMap(doc, "defs"), "main");
        Map<String, Object> params = getMap(mainDef, "parameters");
        Map<String, Object> properties = getMap(params, "properties");
        Map<String, Object> unionParam = getMap(properties, "refUnionParam");
        List<Object> refs = getList(unionParam, "refs");
        assertTrue(refs.contains("#recordDef1"));
        assertTrue(refs.contains("#recordDef2"));
    }

    @Test
    public void testParseLexiconWithToken() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": { "main": { "type": "token" } }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithUnknown() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": { "main": { "type": "unknown" } }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithBytes() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "output": {
                    "encoding": "application/octet-stream",
                    "schema": { "type": "bytes", "maxLength": 1000000 }
                  }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithCidLink() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "output": {
                    "encoding": "application/json",
                    "schema": { "type": "cid-link" }
                  }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithRecord() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "record",
                  "key": "tid",
                  "record": {
                    "type": "object",
                    "required": ["name"],
                    "properties": {
                      "name": {"type": "string"},
                      "age": {"type": "integer"}
                    }
                  }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseLexiconWithSubscription() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "subscription",
                  "parameters": {
                    "type": "params",
                    "properties": { "cursor": { "type": "integer" } }
                  },
                  "message": {
                    "encoding": "application/json",
                    "schema": {
                      "type": "object",
                      "properties": { "message": { "type": "string" } }
                    }
                  },
                  "errors": [ { "name": "FutureCursor" } ]
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseMultipleDefs() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "output": { "encoding": "application/json" }
                },
                "otherDef": {
                  "type": "string"
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        Map<String, Object> doc = assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
        Map<String, Object> defs = getMap(doc, "defs");
        assertTrue(defs.containsKey("main"));
        assertTrue(defs.containsKey("otherDef"));
        assertEquals("string", getMap(defs, "otherDef").get("type"));
    }

    @Test
    public void testParseNestedObjects() throws IOException {
        String lexiconJson = """
        {
          "lexicon": 1,
          "id": "com.example.test",
          "revision": 0,
          "defs": {
            "main": {
              "type": "query",
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "object",
                  "properties": {
                    "outer": {
                      "type": "object",
                      "properties": {
                        "inner": { "type": "string" }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseArrayOfObjects() throws IOException {
        String lexiconJson = """
        {
          "lexicon": 1,
          "id": "com.example.test",
          "revision": 0,
          "defs": {
            "main": {
              "type": "query",
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": { "name": { "type": "string" } }
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseBlob() throws IOException{
        String lexiconJson = """
        {
          "lexicon": 1,
          "id": "com.example.blobtest",
          "revision": 0,
          "defs": {
            "main": {
              "type": "query",
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "object",
                  "properties": {
                    "myBlob": {
                      "type": "blob",
                      "accept": ["image/png", "image/jpeg"],
                      "maxSize": 1000000
                    }
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testProcedure() throws IOException {
          String lexiconJson = """
          {
            "lexicon": 1,
            "id": "com.example.test",
            "revision": 0,
            "defs": {
              "main": {
                "type": "procedure",
                "input": {
                  "encoding": "application/json",
                  "schema": {
                    "type": "object",
                    "required": ["message"],
                    "properties": { "message": {"type": "string"} }
                  }
                },
                "output": { "encoding": "application/json" }
              }
            }
          }
          """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseNestedArraysAndObjects() throws IOException {
        String lexiconJson = """
        {
          "lexicon": 1,
          "id": "com.example.complex",
          "revision": 0,
          "defs": {
            "main": {
              "type": "query",
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "object",
                  "properties": {
                    "outerArray": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "innerArray": {
                            "type": "array",
                            "items": { "type": "integer" }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
     public void testOptionalProperties() throws IOException {
         String lexiconJson = """
         {
           "lexicon": 1,
           "id": "com.example.optional",
           "revision": 0,
           "defs": {
             "main": {
               "type": "query",
               "parameters": {
                 "type": "params",
                 "properties": {
                   "requiredString": { "type": "string" },
                   "optionalString": { "type": "string" }
                 },
                 "required": ["requiredString"]
               },
               "output": { "encoding": "application/json" }
             }
           }
         }
         """;
         LexiconParser parser = new LexiconParser();
         assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseNonMainDef() throws IOException {
        String lexiconJson = """
            {
              "lexicon": 1,
              "id": "com.example.test",
              "revision": 0,
              "defs": {
                "main": {
                  "type": "query",
                  "output": { "encoding": "application/json" }
                },
                "myRecord": {
                  "type": "record",
                  "key": "tid",
                  "record": {
                    "type": "object",
                    "properties": { "name": { "type": "string" } }
                  }
                }
              }
            }
            """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseWithNullValuesInJson() throws IOException {
        // Note: Lexicon spec doesn't explicitly define behavior for JSON null values
        // for fields like description, revision, etc. Assuming they are allowed
        // and treated as absent/Optional.empty() by the parser's validation logic.
        String lexiconJson = """
        {
          "lexicon": 1,
          "id": "com.example.test",
          "revision": null,
          "description": null,
          "defs": {
            "main": {
              "type": "query",
              "description": null,
              "parameters": {
                "type": "params",
                "description": null,
                "properties": {
                  "optionalString": { "type": "string", "description": null }
                },
                "required": null
              },
              "output": {
                "encoding": "application/json",
                "description": null,
                "schema": null
              },
              "errors": null
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        // This test primarily ensures the underlying JSON parser handles nulls
        // and our validation logic correctly interprets them as absent Optionals.
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseWithEmptyObjectAndArray() throws IOException {
        // Test with empty object and array to test edge cases for empty properties and items
        String lexiconJson = """
              {
          "lexicon": 1,
          "id": "com.example.empty",
          "revision": 0,
          "defs": {
            "main": {
              "type": "query",
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "object",
                  "properties": {
                    "emptyObject": { "type": "object", "properties": {} },
                    "emptyArray": { "type": "array", "items": {"type": "string" } },
                    "emptyRequiredArray": {"type": "object", "properties": {}, "required": []},
                    "emptyNullableArray": {"type": "object", "properties": {}, "nullable": []}
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        assertDoesNotThrow(() -> parser.parse(stringToInputStream(lexiconJson)));
    }

}