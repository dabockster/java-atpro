package com.atproto.lexicon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LexiconParser {
    private final ObjectMapper objectMapper;

    public LexiconParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse a lexicon definition string
     * @param lexiconJson JSON string containing the lexicon definition
     * @return true if parsing was successful, false otherwise
     * @throws IOException if there's an error parsing the JSON
     */
    public boolean parse(String lexiconJson) throws IOException {
        JsonNode node = objectMapper.readTree(lexiconJson);
        return validateLexiconNode(node);
    }

    /**
     * Validate a lexicon JSON node
     * @param node JSON node to validate
     * @return true if the lexicon is valid
     */
    private boolean validateLexiconNode(JsonNode node) {
        // Check required fields
        if (!node.has("$schema") || !node.has("lexicon") || !node.has("id") || !node.has("type")) {
            return false;
        }

        // Validate schema version
        JsonNode lexiconNode = node.get("lexicon");
        if (!lexiconNode.isNumber() || lexiconNode.asInt() != 1) {
            return false;
        }

        // Validate type
        String type = node.get("type").asText();
        return isValidType(type);
    }

    /**
     * Validate a type definition
     * @param lexiconJson JSON string containing the type definition
     * @return true if the type definition is valid
     * @throws IOException if there's an error parsing the JSON
     */
    public boolean validateTypeDefinition(String lexiconJson) throws IOException {
        JsonNode node = objectMapper.readTree(lexiconJson);
        String type = node.get("type").asText();
        return isValidType(type);
    }

    /**
     * Check if a type is valid
     * @param type Type string to validate
     * @return true if the type is valid
     */
    private boolean isValidType(String type) {
        // List of valid types according to ATProtocol lexicon
        String[] validTypes = {"string", "number", "boolean", "object", "array", "union", "ref"};
        for (String validType : validTypes) {
            if (validType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a JSON schema from a lexicon definition
     * @param lexiconJson JSON string containing the lexicon definition
     * @return JSON schema as string
     * @throws IOException if there's an error generating the schema
     */
    public String generateSchema(String lexiconJson) throws IOException {
        JsonNode node = objectMapper.readTree(lexiconJson);
        if (!validateLexiconNode(node)) {
            throw new LexiconValidationException("Invalid lexicon definition");
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("title", node.get("id").asText());
        schema.put("type", node.get("type").asText());

        if (node.has("properties")) {
            ObjectNode properties = objectMapper.createObjectNode();
            node.get("properties").fields().forEachRemaining(entry -> {
                properties.set(entry.getKey(), entry.getValue());
            });
            schema.set("properties", properties);
        }

        return schema.toString();
    }

    /**
     * Validate schema evolution from old to new version
     * @param oldLexicon JSON string of the old lexicon version
     * @param newLexicon JSON string of the new lexicon version
     * @return true if the evolution is valid
     * @throws IOException if there's an error parsing the JSON
     */
    public boolean validateSchemaEvolution(String oldLexicon, String newLexicon) throws IOException {
        JsonNode oldNode = objectMapper.readTree(oldLexicon);
        JsonNode newNode = objectMapper.readTree(newLexicon);

        // Check if both lexicons have the same ID
        if (!oldNode.get("id").asText().equals(newNode.get("id").asText())) {
            return false;
        }

        // Check if required fields are maintained
        if (oldNode.has("properties")) {
            JsonNode oldProps = oldNode.get("properties");
            JsonNode newProps = newNode.get("properties");
            
            if (newProps == null) {
                return false;
            }

            for (String fieldName : oldProps.fieldNames()) {
                if (!newProps.has(fieldName)) {
                    return false;
                }
            }
        }

        return true;
    }
}
