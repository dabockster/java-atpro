package com.atproto.lexicon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LexiconParserTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LexiconParser lexiconParser;

    private static final String TEST_LEXICON = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Name field\"}}}";

    @BeforeEach
    void setUp() {
        lexiconParser = new LexiconParser();
    }

    @Test
    public void testLexiconParsing() throws IOException {
        // Test valid lexicon parsing
        JsonNode schema = objectMapper.readTree(TEST_LEXICON);
        when(objectMapper.readTree(TEST_LEXICON)).thenReturn(schema);

        assertTrue(lexiconParser.parse(TEST_LEXICON));

        // Test invalid lexicon (missing required fields)
        String invalidLexicon = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\"}";
        assertFalse(lexiconParser.parse(invalidLexicon));

        // Test invalid JSON
        assertThrows(JsonParseException.class, () -> {
            lexiconParser.parse("invalid_json");
        });
    }

    @Test
    public void testTypeValidation() throws IOException {
        // Test valid type definition
        assertTrue(lexiconParser.validateTypeDefinition(TEST_LEXICON));

        // Test invalid type (unknown type)
        String invalidType = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\",\"type\":\"unknown\"}";
        assertFalse(lexiconParser.validateTypeDefinition(invalidType));

        // Test invalid type (missing type)
        String missingType = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\"}";
        assertFalse(lexiconParser.validateTypeDefinition(missingType));

        // Additional type validation test
        String lexicon = "{\"$schema\": \"https://atproto.com/lexicon-1.json\",\n" +
                "            \"lexicon\": 1,\n" +
                "            \"id\": \"com.atproto.identity.resolveHandle\",\n" +
                "            \"parameters\": {\n" +
                "                \"required\": [\"handle\"]\n" +
                "            },\n" +
                "            \"output\": {\n" +
                "                \"encoding\": \"application/json\",\n" +
                "                \"schema\": {\n" +
                "                    \"required\": [\"did\"]\n" +
                "                }\n" +
                "            }\n" +
                "        }";

        LexiconDefinition definition = lexiconParser.parse(lexicon);
        Map<String, Object> data = Map.of(
                "handle", "example.com",
                "invalidField", "value"
        );

        try {
            definition.validate(data);
            fail("Should have thrown validation error");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("invalidField"));
        }
    }

    @Test
    public void testSchemaGeneration() throws IOException {
        // Test schema generation from lexicon
        String generatedSchema = lexiconParser.generateSchema(TEST_LEXICON);
        assertNotNull(generatedSchema);

        // Test invalid lexicon schema generation
        String invalidLexicon = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\"}";
        assertThrows(LexiconValidationException.class, () -> {
            lexiconParser.generateSchema(invalidLexicon);
        });

        // Test schema evolution
        String evolvedLexicon = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Name field\"},\"age\":{\"type\":\"number\",\"description\":\"Age field\"}}}";
        assertTrue(lexiconParser.validateSchemaEvolution(TEST_LEXICON, evolvedLexicon));

        // Additional schema generation test
        String lexicon = "{\"$schema\": \"https://atproto.com/lexicon-1.json\",\n" +
                "            \"lexicon\": 1,\n" +
                "            \"id\": \"com.atproto.identity.resolveHandle\",\n" +
                "            \"parameters\": {\n" +
                "                \"required\": [\"handle\"]\n" +
                "            },\n" +
                "            \"output\": {\n" +
                "                \"encoding\": \"application/json\",\n" +
                "                \"schema\": {\n" +
                "                    \"required\": [\"did\"]\n" +
                "                }\n" +
                "            }\n" +
                "        }";

        LexiconDefinition definition = lexiconParser.parse(lexicon);
        String schema = definition.generateSchema();
        assertNotNull(schema);
        assertTrue(schema.contains("handle"));
        assertTrue(schema.contains("did"));
    }

    @Test
    public void testRecursiveTypeValidation() {
        String lexicon = "{\"$schema\": \"https://atproto.com/lexicon-1.json\",\n" +
                "            \"lexicon\": 1,\n" +
                "            \"id\": \"com.atproto.identity.resolveHandle\",\n" +
                "            \"parameters\": {\n" +
                "                \"required\": [\"handle\"]\n" +
                "            },\n" +
                "            \"output\": {\n" +
                "                \"encoding\": \"application/json\",\n" +
                "                \"schema\": {\n" +
                "                    \"required\": [\"did\"],\n" +
                "                    \"properties\": {\n" +
                "                        \"nested\": {\n" +
                "                            \"type\": \"object\",\n" +
                "                            \"required\": [\"value\"]\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }";

        LexiconDefinition definition = lexiconParser.parse(lexicon);
        Map<String, Object> data = Map.of(
                "handle", "example.com",
                "nested", Map.of("value", "test")
        );

        assertTrue(definition.validate(data).isEmpty());

        data = Map.of(
                "handle", "example.com",
                "nested", Map.of("invalid", "test")
        );

        assertFalse(definition.validate(data).isEmpty());
    }

    @Test
    public void testErrorHandling() {
        // Test invalid JSON parsing
        assertThrows(JsonParseException.class, () -> {
            lexiconParser.parse("invalid_json");
        });

        // Test invalid lexicon format
        assertThrows(LexiconValidationException.class, () -> {
            lexiconParser.parse("{\"invalid\":\"lexicon\"}");
        });

        // Test invalid type definition
        assertThrows(LexiconValidationException.class, () -> {
            String invalidType = "{\"$schema\":\"https://atproto.com/lexicon-1.json\",\"lexicon\":1,\"id\":\"com.example.schema\",\"description\":\"Test schema\",\"type\":\"unknown\"}";
            lexiconParser.validateTypeDefinition(invalidType);
        });

        // Additional error handling test
        String invalidJson = "{\"$schema\": \"https://atproto.com/lexicon-1.json\",\n" +
                "            \"lexicon\": 1,\n" +
                "            \"id\": \"com.atproto.identity.resolveHandle\",\n" +
                "            \"parameters\": {\n" +
                "                \"required\": [\"handle\"]\n" +
                "        }"; // Missing closing brace

        try {
            lexiconParser.parse(invalidJson);
            fail("Should have thrown JSON parsing error");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("JSON"));
        }
    }
}
