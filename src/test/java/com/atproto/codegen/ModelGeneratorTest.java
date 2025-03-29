package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

public class ModelGeneratorTest {

    @Test
    public void testAppliesStringLengthConstraintWithMaxLength() {
        // Longer than two UTF8 characters
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abcde"));
        });

        // Two to four UTF8 characters
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("ab"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("\u0301")); // Combining acute accent (2 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("a\u0301")); // 'a' + combining acute accent (1 + 2 bytes = 3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("aé")); // 'a' (1 byte) + 'é' (2 bytes) = 3 bytes
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abc"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("一")); // CJK character (3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("\uD83D")); // Unpaired high surrogate (3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abcd"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("éé")); // 'é' + 'é' (2 + 2 bytes = 4 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("aaé")); // 1 + 1 + 2 = 4 bytes
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("👋")); // 4 bytes
        });

        // Shorter than two UTF8 characters
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord(""));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("a"));
        });
    }

    @Test
    public void testAppliesStringLengthConstraintWithMaxGraphemes() {
        // Longer than four graphemes
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("áb́ćd́é"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("😀😀😀😀😀"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("ab😀de"));
        });

        // Four graphemes or less
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("áb́ćd́"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("😀😀😀😀"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("abc"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("a😀b"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("aé"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("éé"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("👋"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord(""));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("a"));
        });
    }

    @Test
    public void testAppliesStringLengthConstraintWithMinGraphemes() {
        // Shorter than two graphemes
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord(""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("a"));
        });

        // Two graphemes or more
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("ab"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("aé"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("éé"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("👋"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀😀"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀😀😀"));
        });
    }

    @Test
    public void testAppliesStringLengthConstraintWithMinLength() {
        // Shorter than two UTF8 characters
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord(""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("a"));
        });

        // Two to four UTF8 characters
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("ab"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("\u0301")); // Combining acute accent (2 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("a\u0301")); // 'a' + combining acute accent (1 + 2 bytes = 3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("aé")); // 'a' (1 byte) + 'é' (2 bytes) = 3 bytes
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("abc"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("一")); // CJK character (3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("\uD83D")); // Unpaired high surrogate (3 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("abcd"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("éé")); // 'é' + 'é' (2 + 2 bytes = 4 bytes)
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("aaé")); // 1 + 1 + 2 = 4 bytes
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord("👋")); // 4 bytes
        });
    }

    @Test
    public void testAppliesStringPatternConstraint() {
        // Pattern does not match
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("abc123"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("123abc"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("!@#abc"));
        });

        // Pattern matches
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("abc"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("def"));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord("xyz"));
        });
    }

    @Test
    public void testAppliesIntegerConstraintWithMaxValue() {
        // Greater than max value
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(11));
        });

        // Less than or equal to max value
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(10));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(5));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(0));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(-5));
        });
    }

    @Test
    public void testAppliesIntegerConstraintWithMinValue() {
        // Less than min value
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-11));
        });

        // Greater than or equal to min value
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-10));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-5));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(0));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(5));
        });
    }

    @Test
    public void testAppliesIntegerConstraintWithExclusiveMaxValue() {
        // Greater than or equal to exclusive max value
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(10));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(11));
        });

        // Less than exclusive max value
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(5));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(0));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(-5));
        });
    }

    @Test
    public void testAppliesIntegerConstraintWithExclusiveMinValue() {
        // Less than or equal to exclusive min value
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-10));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-11));
        });

        // Greater than exclusive min value
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-5));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(0));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(5));
        });
    }

    @Test
    public void testAppliesBooleanConstraint() {
        // Invalid boolean value
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord("true"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord("false"));
        });

        // Valid boolean value
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord(true));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord(false));
        });
    }

    @Test
    public void testAppliesObjectConstraint() {
        // Invalid object structure
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.objectValue", new TestRecord(new TestRecord("invalid")));
        });

        // Valid object structure
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.objectValue", new TestRecord(new TestObject("valid", 10)));
        });
    }

    @Test
    public void testAppliesArrayConstraintWithMinItems() {
        // Fewer items than minItems
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a")));
        });

        // Equal to or more than minItems
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a", "b")));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a", "b", "c")));
        });
    }

    @Test
    public void testAppliesArrayConstraintWithMaxItems() {
        // More items than maxItems
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b", "c", "d")));
        });

        // Equal to or fewer than maxItems
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b", "c")));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b")));
        });
        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a")));
        });
    }

    @Test
    public void testAppliesNumberConstraintWithExclusiveMinimum() {
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.numberExclusiveMinimum", new TestRecord(5));
        });

        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.numberExclusiveMinimum", new TestRecord(6));
        });
    }

    @Test
    public void testAppliesNumberConstraintWithExclusiveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> {
            ModelGenerator.validateRecord("com.example.numberExclusiveMaximum", new TestRecord(10));
        });

        assertDoesNotThrow(() -> {
            ModelGenerator.validateRecord("com.example.numberExclusiveMaximum", new TestRecord(9));
        });
    }

    // Placeholder for the TestRecord class
    private static class TestRecord {
        private final Object value;

        public TestRecord(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    // Placeholder for the TestObject class
    private static class TestObject {
        private final String name;
        private final int number;

        public TestObject(String name, int number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public int getNumber() {
            return number;
        }
    }
}