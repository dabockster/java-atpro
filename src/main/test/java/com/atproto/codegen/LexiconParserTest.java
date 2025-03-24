// src/test/java/com/atproto/codegen/LexiconParserTest.java
package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;

import com.atproto.lexicon.models.*;

import main.java.com.atproto.codegen.LexiconParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class LexiconParserTest {

    private InputStream stringToInputStream(String str) {
        return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testParseEmptyLexicon() throws IOException {
        String lexiconJson = "{}"; // Simplest possible Lexicon (invalid; missing id, and defs)
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
    }

    @Test
    public void testParseMinimalValidLexicon() throws IOException {
        String lexiconJson =
            """
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
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));

        assertNotNull(doc);
        assertEquals(1, doc.getLexicon());
        assertEquals("com.example.test", doc.getId());
        assertEquals(0, (int) doc.getRevision().get()); // Need to unwrap optional int
        assertNotNull(doc.getDefs());
        assertTrue(doc.getDefs().containsKey("main"));

        LexDefinition mainDef = doc.getDefs().get("main");
        assertEquals("query", mainDef.getType());
        assertTrue(mainDef instanceof LexXrpcQuery); // Check the specific type
        LexXrpcQuery query = (LexXrpcQuery) mainDef; // Downcast is safe after check
        assertNotNull(query.getOutput());
        assertEquals("application/json", query.getOutput().get().getEncoding()); // Unwrap optional
        assertFalse(query.getOutput().get().getSchema().isPresent()); // Test Optional works as-tested (no
        // schema)
    }

    @Test
    public void testParseLexiconWithDescription() throws IOException {
        String lexiconJson =
            """
                {
                "lexicon": 1,
                "id": "com.example.test",
                "revision": 1,
                "description": "A test lexicon.",
                "defs": {
                    "main": {
                    "type": "query",
                    "description": "A test query.",
                    "output": {
                        "encoding": "application/json"
                    }
                    }
                }
                }
                """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        assertEquals("A test lexicon.", doc.getDescription().get()); // Test Optional exists
        LexXrpcQuery mainDef = (LexXrpcQuery) doc.getDefs().get("main");
        assertEquals("A test query.", mainDef.getDescription().get()); // Sub-value Optional
    }

    @Test
    public void testParseLexiconWithInteger() throws IOException {
        String lexiconJson =
            """
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
                        "output": {
                            "encoding": "application/json"
                        }
                    }
                }
            }
            """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");

        LexXrpcParameters params = query.getParameters().get(); // Test Optional existence
        assertNotNull(params);

        LexObject props = (LexObject) params; // Parameters IS-A object, so check that it exists
        assertNotNull(props.getProperties());
        assertTrue(props.getProperties().containsKey("count"));

        LexPrimitive integerProp = (LexPrimitive) props.getProperties().get("count");
        assertTrue(integerProp instanceof LexInteger);
        LexInteger count = (LexInteger) integerProp;

        assertEquals(1, (int) count.getMinimum().get()); // Unwrap optional int
        assertEquals(100, (int) count.getMaximum().get());
        assertEquals(10, (int) count.getDefault().get());
    }

    @Test
    public void testParseLexiconWithString() throws IOException {
        String lexiconJson =
            """
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
                "output": {
                    "encoding": "application/json"
                }
                }
            }
            }
            """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");

        LexXrpcParameters params = query.getParameters().get();
        assertNotNull(params);
        LexObject props = (LexObject) params; // Validate parameters, is in-fact, an Object
        assertNotNull(props.getProperties());
        assertTrue(props.getProperties().containsKey("name"));

        LexPrimitive stringProp = (LexPrimitive) props.getProperties().get("name");
        assertTrue(stringProp instanceof LexString); // Validate type
        LexString name = (LexString) stringProp; // Cast is safe, after validation

        assertEquals(100, (int) name.getMaxLength().get());
        assertEquals(1, (int) name.getMinLength().get());
        assertEquals("atproto", name.getConst().get());
        assertEquals("atproto", name.getDefault().get()); // Test default and const together
    }

    @Test
    public void testParseLexiconWithBoolean() throws IOException {
        String lexiconJson =
            """
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
                "output": {
                    "encoding": "application/json"
                }
                }
            }
            }
            """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");

        LexXrpcParameters params = query.getParameters().get(); // Test Optional existence
        assertNotNull(params);
        LexObject props = (LexObject) params; // Validate parameters, is in-fact, an Object
        assertNotNull(props.getProperties());
        assertTrue(props.getProperties().containsKey("enabled"));

        LexPrimitive booleanProp = (LexPrimitive) props.getProperties().get("enabled");
        assertTrue(booleanProp instanceof LexBoolean);
        LexBoolean enabled = (LexBoolean) booleanProp;

        assertEquals(false, enabled.getDefault().get()); // Test default value.
    }

    @Test
    public void testParseLexiconWithArray() throws IOException {
        String lexiconJson =
            """
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
                            "items": {
                                "type": "string"
                            },
                            "maxLength": 10,
                            "minLength": 1
                            }
                        }
                    },
                        "output": {
                            "encoding": "application/json"
                    }
                    }
                }
                }
                """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");

        LexXrpcParameters params = query.getParameters().get(); // Test Optional existence
        assertNotNull(params);
        LexObject props = (LexObject) params; // Validate parameters, is in-fact, an Object
        assertNotNull(props.getProperties());
        assertTrue(props.getProperties().containsKey("names"));

        LexArray arrayProp = (LexArray) props.getProperties().get("names");
        assertTrue(arrayProp instanceof LexArray); // Validate type
        LexString items = (LexString) arrayProp.getItems(); // Cast is safe, after validation

        assertEquals(10, (int) arrayProp.getMaxLength().get());
        assertEquals(1, (int) arrayProp.getMinLength().get());
        assertEquals("string", items.getType()); // Validate nested item
    }

    @Test
    public void testParseLexiconWithRef() throws IOException { // Test a basic ref.
        String lexiconJson =
            """
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
                                "refParam": {
                                    "type": "ref",
                                    "ref": "#recordDef"
                                }
                            }
                        },
                        "output": {
                            "encoding": "application/json"
                        }
                    },
                    "recordDef": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string"
                            }
                        }
                    }
                }
            }
            """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        LexXrpcParameters params = query.getParameters().get();
        LexObject props = (LexObject) params;

        LexRef ref = (LexRef) props.getProperties().get("refParam");
        assertEquals("#recordDef", ref.getRef());
    }

    @Test
    public void testParseLexiconWithUnion() throws IOException { // Test ref union
        String lexiconJson =
            """
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
                                        "refs": [
                                            "#recordDef1",
                                            "#recordDef2"
                                    ]
                                }
                            }
                        },
                        "output": {
                            "encoding": "application/json"
                        }
                    },
                    "recordDef1": {
                        "type": "object",
                        "properties": {
                            "name1": {"type": "string"}
                        }
                    },
                    "recordDef2": {
                        "type": "object",
                        "properties": {
                            "name2": {"type": "string"}
                        }
                    }
                }
            }
            """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        LexXrpcParameters params = query.getParameters().get();
        LexObject props = (LexObject) params;

        LexRefUnion refUnion = (LexRefUnion) props.getProperties().get("refUnionParam");
        assertNotNull(refUnion);
        // Unions always return at least one entry, even if empty
        assertTrue(refUnion.getRefs().contains("#recordDef1")); // Test both contained
        assertTrue(refUnion.getRefs().contains("#recordDef2"));
    }

    @Test
    public void testParseLexiconWithToken() throws IOException {
        String lexiconJson =
            """
                {
                "lexicon": 1,
                "id": "com.example.test",
                "revision": 0,
                "defs": {
                    "main": {
                    "type": "token"
                    }
                }
                }
                """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexToken token = (LexToken) doc.getDefs().get("main");
        assertEquals("token", token.getType()); // Should be a token.
    }

    @Test
    public void testParseLexiconWithUnknown() throws IOException { // Test Unknown
        String lexiconJson =
            """
                {
                "lexicon": 1,
                "id": "com.example.test",
                "revision": 0,
                "defs": {
                    "main": {
                    "type": "unknown"
                    }
                }
                }
                """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexUnknown unknown = (LexUnknown) doc.getDefs().get("main");
        assertEquals("unknown", unknown.getType());
    }

    @Test
    public void testParseLexiconWithBytes() throws IOException { // Test Bytes
        String lexiconJson =
            """
                {
                    "lexicon": 1,
                    "id": "com.example.test",
                    "revision": 0,
                    "defs": {
                    "main": {
                        "type": "query",
                        "output": {
                        "encoding": "application/octet-stream",
                        "schema": {
                            "type": "bytes"
                        }
                        }
                    }
                    }
                }
                """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        LexXrpcBody schema = query.getOutput().get().getSchema().get(); // Test Optionals along the way

        LexBytes bytes = (LexBytes) schema; // Cast is safe, after validation
        assertEquals("bytes", bytes.getType());
    }

    @Test
    public void testParseLexiconWithCidLink() throws IOException { // Test CID Link
        String lexiconJson =
            """
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
                            "type": "cid-link"                       
                            }
                        }
                    }
                    }
                }
                """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        LexXrpcBody schema = query.getOutput().get().getSchema().get(); // Test Optional exists

        LexCidLink cidLink = (LexCidLink) schema; // Cast is safe, after validation
        assertEquals("cid-link", cidLink.getType());
    }

    @Test
    public void testParseLexiconWithRecord() throws IOException { // Test a full Record definition
        String lexiconJson =
            """
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
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexRecord record = (LexRecord) doc.getDefs().get("main");
        assertEquals("record", record.getType());
        assertEquals("tid", record.getKey());

        LexObject recordObject = (LexObject) record.getRecord(); // Validate the contained object
        assertNotNull(recordObject);
        assertTrue(recordObject.getRequired().contains("name")); // Test required properties
        assertFalse(recordObject.getRequired().contains("age"));
        assertTrue(recordObject.getProperties().containsKey("name"));
        assertTrue(recordObject.getProperties().containsKey("age"));
        assertTrue(recordObject.getProperties().get("name") instanceof LexString); // Check nested types
        assertTrue(recordObject.getProperties().get("age") instanceof LexInteger);
    }

    @Test
    public void testParseLexiconWithSubscription() throws IOException { // Test Subscription
        String lexiconJson =
            """
                {
                    "lexicon": 1,
                    "id": "com.example.test",
                    "revision": 0,
                    "defs": {
                    "main": {
                        "type": "subscription",
                        "parameters": {
                            "type": "params",
                            "properties": {
                                "cursor": { "type": "integer" }
                            }
                        },
                        "message": {
                        "encoding": "application/json",
                        "schema": {
                            "type": "object",
                            "properties": {
                                "message": { "type": "string" }
                            }
                        }
                        }
                    }
                    }
                }
                """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexSubscription sub = (LexSubscription) doc.getDefs().get("main");
        LexXrpcParameters params = sub.getParameters().get();

        LexObject props = (LexObject) params;
        assertTrue(props.getProperties().containsKey("cursor"));
        assertTrue(props.getProperties().get("cursor") instanceof LexInteger); // Validate sub-values

        LexXrpcBody message = sub.getMessage().get(); // Test that message exists in Union
        assertEquals("application/json", message.getEncoding());
        LexObject messageSchema = (LexObject) message.getSchema().get();
        assertTrue(messageSchema.getProperties().containsKey("message"));
        assertTrue(
            messageSchema.getProperties().get("message") instanceof LexString); // Validate sub-values
    }

    // Test for invalid Lexicon versions
    @Test
    public void testInvalidLexiconVersion() {
        String lexiconJson =
            """
                {
                "lexicon": 2,
                "id": "com.example.test",
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
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
    }
    // Test for missing required fields in different parts of lexicon
    @Test
    public void testMissingRequiredFields() {
        String lexiconJson =
            """
            {
                "lexicon": 1,
                "id": "com.example.test",
                "defs": {
                "main": {
                    "type": "record",
                    "record": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string"}
                        }
                    }
                }
                }
            }
            """; // Missing required "key: tid"

        LexiconParser parser = new LexiconParser();
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse(stringToInputStream(lexiconJson))); // Expecting parse to fail
    }

    // Test for invalid JSON structure
    @Test
    public void testInvalidJson() {
        String lexiconJson = "{,}"; // Invalid JSON
        LexiconParser parser = new LexiconParser();
        assertThrows(IOException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
    }

    // Test for invalid Lexicon type.
    @Test
    public void testInvalidLexiconType() throws IOException {
        String lexiconJson =
            """
                {
                "lexicon": 1,
                "id": "com.example.test",
                "revision": 0,
                "defs": {
                    "main": {
                    "type": "invalidType",
                    "output": {
                        "encoding": "application/json"
                    }
                    }
                }
                }
            """;
        LexiconParser parser = new LexiconParser();

        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
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
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        assertTrue(doc.getDefs().containsKey("main"));
        assertTrue(doc.getDefs().containsKey("otherDef"));
        assertTrue(doc.getDefs().get("otherDef") instanceof LexString);
    }

    @Test
    public void testParseNestedObjects() throws IOException { // Nested Objects
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
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery)doc.getDefs().get("main");
        LexXrpcBody schema = query.getOutput().get().getSchema().get();
        LexObject outer = (LexObject) schema;
        LexObject inner = (LexObject) outer.getProperties().get("outer");
        LexString innerString = (LexString) inner.getProperties().get("inner");
        assertEquals("string", innerString.getType());
    }

    @Test
    public void testParseArrayOfObjects() throws IOException { // Array of objects
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
                                "properties": {
                                    "name": { "type": "string" }
                                }
                            }
                        }
                    }
                }
            }
        }
        """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery)doc.getDefs().get("main");
        LexXrpcBody schema = query.getOutput().get().getSchema().get();
        LexArray array = (LexArray)schema;
        LexObject arrayItem = (LexObject)array.getItems();
        assertTrue(arrayItem.getProperties().get("name") instanceof LexString); // Access the name
    }

    @Test
    public void testParseBlob() throws IOException{ //Explicit blob test
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
                      "type": "blob"
                    }
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery)doc.getDefs().get("main");
        LexXrpcBody output = query.getOutput().get();
        LexObject schema = (LexObject)output.getSchema().get();
        LexBlob blob = (LexBlob)schema.getProperties().get("myBlob");

        assertNotNull(blob);

    }

    @Test
    public void testProcedure() throws IOException { //Test a Procedure, which uses an "input"
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
                        "properties": {
                            "message": {"type": "string"}
                        }
                     }
                },
                "output": {
                  "encoding": "application/json"
                }
              }
            }
          }
          """;
            LexiconParser parser = new LexiconParser();
            LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
            LexXrpcProcedure procedure = (LexXrpcProcedure)doc.getDefs().get("main");

            LexXrpcBody input = procedure.getInput().get();
            assertEquals("application/json", input.getEncoding());
            LexObject inputSchema = (LexObject) input.getSchema().get(); //Check for presence and cast
            LexString message = (LexString)inputSchema.getProperties().get("message"); //Check for string and allow cast
            assertEquals("string", message.getType());
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
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        LexXrpcBody output = query.getOutput().get();
        LexObject schema = (LexObject) output.getSchema().get();
        LexArray outerArray = (LexArray) schema.getProperties().get("outerArray");
        LexObject innerObject = (LexObject) outerArray.getItems();
        LexArray innerArray = (LexArray) innerObject.getProperties().get("innerArray");
        assertTrue(innerArray.getItems() instanceof LexInteger); // Check nested array type.
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
                 "output": {
                     "encoding": "application/json"
                 }
             }
           }
         }
         """;

         LexiconParser parser = new LexiconParser();
         LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));

         LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
         LexXrpcParameters params = query.getParameters().get(); // Test Optional existence
         LexObject props = (LexObject) params; // Validate parameters, is in-fact, an Object

         assertTrue(props.getProperties().containsKey("requiredString"));  //Should exist
         assertTrue(props.getProperties().containsKey("optionalString"));  //Should also exist
         assertTrue(props.getRequired().contains("requiredString"));    // Test required

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
                  "output": {
                    "encoding": "application/json"
                  }
                },
                "myRecord": {
                    "type": "record",
                    "key": "tid",
                    "record": {
                        "type": "object",
                        "properties": {
                            "name": { "type": "string" }
                        }
                    }
                }
              }
            }
            """;

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexRecord myRecord = (LexRecord) doc.getDefs().get("myRecord"); // Access non-main def
        assertNotNull(myRecord);
        assertEquals("record", myRecord.getType());
        assertEquals("tid", myRecord.getKey());
    }

    @Test
    public void testParseWithNullValues() throws IOException {
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
                  "optionalString": { "type": "string" }
                }
              },
              "output": {
                "encoding": "application/json",
                "schema": {
                  "type": "object",
                  "properties": {
                    "nullableProp": { "type": "string" },
                    "anotherNullableProp": { "type": "integer" }
                  },
                    "required": []
                }
              }
            }
          }
        }
        """;

        //This tests to make sure we don't accidentally throw an error on valid Lexicon, because there is no "nullable" field (or a field indicating nullability).

        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));

        LexXrpcQuery query = (LexXrpcQuery) doc.getDefs().get("main");
        //Check that optionals correctly return without error
        LexXrpcParameters params = query.getParameters().get();
        LexObject props = (LexObject) params;

        assertNotNull(props.getProperties().get("optionalString"));
        assertTrue(props.getProperties().get("optionalString") instanceof LexString);

        //Check output schema
        LexXrpcBody output = query.getOutput().get();
        LexObject outputSchema = (LexObject) output.getSchema().get();
        assertTrue(outputSchema.getProperties().containsKey("nullableProp"));
        assertTrue(outputSchema.getProperties().get("nullableProp") instanceof LexString );   // Make sure types are correct, too
        assertTrue(outputSchema.getProperties().containsKey("anotherNullableProp"));
        assertTrue(outputSchema.getProperties().get("anotherNullableProp") instanceof LexInteger );
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
                    "emptyRequiredArray": {"type": "object", "properties": {}, "required": []}
                  }
                }
              }
            }
          }
        }
        """;
        LexiconParser parser = new LexiconParser();
        LexiconDoc doc = parser.parse(stringToInputStream(lexiconJson));
        LexXrpcQuery query = (LexXrpcQuery)doc.getDefs().get("main"); // Get a definition with nested objects.
        LexXrpcBody outputBody = query.getOutput().get(); // Get the output information
        LexObject schema = (LexObject)outputBody.getSchema().get();   // Get the schema of the output

        LexObject emptyObject = (LexObject) schema.getProperties().get("emptyObject");
        assertNotNull(emptyObject);
        assertTrue(emptyObject.getProperties().isEmpty());           // Validate that the object is empty.
        assertNull(emptyObject.getRequired());

        LexArray emptyArray = (LexArray)schema.getProperties().get("emptyArray");  // Get and check Array
        assertNotNull(emptyArray);
        assertEquals("string", ((LexString)emptyArray.getItems()).getType()); //Verify the inner item type

        LexObject emptyRequiredArray = (LexObject)schema.getProperties().get("emptyRequiredArray");    // Get object
        assertNotNull(emptyRequiredArray);              // Check that nulls are properly returned for empties.
        assertNull(emptyRequiredArray.getRequired());
    }

    @Test
    public void testKnownBadDefinition_MissingDefs() {
        String lexiconJson = """
        {
            "lexicon": 1,
            "id": "com.example.missingdefs"
        }
        """;
        // Missing "defs" entirely.
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
    }

      @Test
    public void testKnownBadDefinition_MissingType() {
        String lexiconJson = """
        {
          "lexicon": 1,
            "id": "com.example.test",
            "revision": 0,
          "defs": {
            "main": {
                 "output": {
                    "encoding": "application/json"
                }
            }
          }
        }
        """;
        // Missing a "type" where there should be one.
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson)));
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
                    "ref": "invalid-ref-format"
                  }
                }
              },
            "output": {
              "encoding": "application/json"
            }
          }
        }
      }
        """;
        //Bad ref format.
        LexiconParser parser = new LexiconParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stringToInputStream(lexiconJson))); // Expecting parse to fail
    }
}