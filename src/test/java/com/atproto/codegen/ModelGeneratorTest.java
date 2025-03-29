package com.atproto.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("Model Generator Tests")
class ModelGeneratorTest {
    @Mock
    private ModelGenerator modelGenerator;

    @BeforeEach
    void setUp() {
        // Initialize mocks and test data if needed
    }

    @Nested
    @DisplayName("String Length Validation")
    class StringLengthTests {
        @Test
        @DisplayName("Validates string length with max length constraint")
        void testAppliesStringLengthConstraintWithMaxLength() {
            // Longer than two UTF8 characters
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abcde"));
            }).isInstanceOf(IllegalArgumentException.class);

            // Two to four UTF8 characters
            assertThatCode(() -> {
                ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("ab"));
            }).doesNotThrowAnyException();
            
            // Additional assertions using AssertJ
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("\u0301"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("a\u0301"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("aé")); // 'a' (1 byte) + 'é' (2 bytes) = 3 bytes
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abc"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("一")); // CJK character (3 bytes)
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("\uD83D")); // Unpaired high surrogate (3 bytes)
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("abcd"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("éé")); // 'é' + 'é' (2 + 2 bytes = 4 bytes)
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("aaé")); // 1 + 1 + 2 = 4 bytes
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("👋")); // 4 bytes
                }).doesNotThrowAnyException()
            );

            // Shorter than two UTF8 characters
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord(""));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLength", new TestRecord("a"));
                }).isInstanceOf(IllegalArgumentException.class)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a"})
        @DisplayName("Validates string length with min length constraint")
        void testAppliesStringLengthConstraintWithMinLength(String shortString) {
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.stringLengthMin", new TestRecord(shortString));
            }).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("String Pattern Validation")
    class StringPatternTests {
        @ParameterizedTest
        @CsvSource({
            "abc123, false",
            "123abc, false",
            "!@#abc, false",
            "abc, true",
            "ABC, true"
        })
        @DisplayName("Validates string pattern constraint")
        void testAppliesStringPatternConstraint(String input, boolean isValid) {
            if (isValid) {
                assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord(input));
                }).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringPattern", new TestRecord(input));
                }).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("String Length Validation with Max Graphemes")
    class StringLengthGraphemeTests {
        @Test
        @DisplayName("Validates string length with max graphemes constraint")
        void testAppliesStringLengthConstraintWithMaxGraphemes() {
            // Longer than four graphemes
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("áb́ćd́é"));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("😀😀😀😀😀"));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("ab😀de"));
                }).isInstanceOf(IllegalArgumentException.class)
            );

            // Four graphemes or less
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("áb́ćd́"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("😀😀😀😀"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("abc"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("a😀b"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("aé"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("éé"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("👋"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord(""));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGrapheme", new TestRecord("a"));
                }).doesNotThrowAnyException()
            );
        }
    }

    @Nested
    @DisplayName("String Length Validation with Min Graphemes")
    class StringLengthGraphemeMinTests {
        @Test
        @DisplayName("Validates string length with min graphemes constraint")
        void testAppliesStringLengthConstraintWithMinGraphemes() {
            // Shorter than two graphemes
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord(""));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("a"));
                }).isInstanceOf(IllegalArgumentException.class)
            );

            // Two graphemes or more
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("ab"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("aé"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("éé"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("👋"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀😀"));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.stringLengthGraphemeMin", new TestRecord("😀😀😀😀😀"));
                }).doesNotThrowAnyException()
            );
        }
    }

    @Nested
    @DisplayName("Integer Validation")
    class IntegerTests {
        @Test
        @DisplayName("Validates integer with max value constraint")
        void testAppliesIntegerConstraintWithMaxValue() {
            // Greater than max value
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(11));
            }).isInstanceOf(IllegalArgumentException.class);

            // Less than or equal to max value
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(10));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(5));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(0));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMax", new TestRecord(-5));
                }).doesNotThrowAnyException()
            );
        }

        @Test
        @DisplayName("Validates integer with min value constraint")
        void testAppliesIntegerConstraintWithMinValue() {
            // Less than min value
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-11));
            }).isInstanceOf(IllegalArgumentException.class);

            // Greater than or equal to min value
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-10));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(-5));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(0));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerMin", new TestRecord(5));
                }).doesNotThrowAnyException()
            );
        }

        @Test
        @DisplayName("Validates integer with exclusive max value constraint")
        void testAppliesIntegerConstraintWithExclusiveMaxValue() {
            // Greater than or equal to exclusive max value
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(10));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(11));
                }).isInstanceOf(IllegalArgumentException.class)
            );

            // Less than exclusive max value
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(5));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(0));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMax", new TestRecord(-5));
                }).doesNotThrowAnyException()
            );
        }

        @Test
        @DisplayName("Validates integer with exclusive min value constraint")
        void testAppliesIntegerConstraintWithExclusiveMinValue() {
            // Less than or equal to exclusive min value
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-10));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-11));
                }).isInstanceOf(IllegalArgumentException.class)
            );

            // Greater than exclusive min value
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(-5));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(0));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.integerExclusiveMin", new TestRecord(5));
                }).doesNotThrowAnyException()
            );
        }
    }

    @Nested
    @DisplayName("Boolean Validation")
    class BooleanTests {
        @Test
        @DisplayName("Validates boolean value")
        void testAppliesBooleanConstraint() {
            // Invalid boolean value
            assertAll(
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord("true"));
                }).isInstanceOf(IllegalArgumentException.class),
                () -> assertThatThrownBy(() -> {
                    ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord("false"));
                }).isInstanceOf(IllegalArgumentException.class)
            );

            // Valid boolean value
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord(true));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.booleanValue", new TestRecord(false));
                }).doesNotThrowAnyException()
            );
        }
    }

    @Nested
    @DisplayName("Object Validation")
    class ObjectTests {
        @Test
        @DisplayName("Validates object structure")
        void testAppliesObjectConstraint() {
            // Invalid object structure
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.objectValue", new TestRecord(new TestRecord("invalid")));
            }).isInstanceOf(IllegalArgumentException.class);

            // Valid object structure
            assertThatCode(() -> {
                ModelGenerator.validateRecord("com.example.objectValue", new TestRecord(new TestObject("valid", 10)));
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Array Validation")
    class ArrayTests {
        @Test
        @DisplayName("Validates array with min items constraint")
        void testAppliesArrayConstraintWithMinItems() {
            // Fewer items than minItems
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a")));
            }).isInstanceOf(IllegalArgumentException.class);

            // Equal to or more than minItems
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a", "b")));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.arrayMinItems", new TestRecord(Arrays.asList("a", "b", "c")));
                }).doesNotThrowAnyException()
            );
        }

        @Test
        @DisplayName("Validates array with max items constraint")
        void testAppliesArrayConstraintWithMaxItems() {
            // More items than maxItems
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b", "c", "d")));
            }).isInstanceOf(IllegalArgumentException.class);

            // Equal to or fewer than maxItems
            assertAll(
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b", "c")));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a", "b")));
                }).doesNotThrowAnyException(),
                () -> assertThatCode(() -> {
                    ModelGenerator.validateRecord("com.example.arrayMaxItems", new TestRecord(Arrays.asList("a")));
                }).doesNotThrowAnyException()
            );
        }
    }

    @Nested
    @DisplayName("Number Validation")
    class NumberTests {
        @Test
        @DisplayName("Validates number with exclusive minimum constraint")
        void testAppliesNumberConstraintWithExclusiveMinimum() {
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.numberExclusiveMinimum", new TestRecord(5));
            }).isInstanceOf(IllegalArgumentException.class);

            assertThatCode(() -> {
                ModelGenerator.validateRecord("com.example.numberExclusiveMinimum", new TestRecord(6));
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Validates number with exclusive maximum constraint")
        void testAppliesNumberConstraintWithExclusiveMaximum() {
            assertThatThrownBy(() -> {
                ModelGenerator.validateRecord("com.example.numberExclusiveMaximum", new TestRecord(10));
            }).isInstanceOf(IllegalArgumentException.class);

            assertThatCode(() -> {
                ModelGenerator.validateRecord("com.example.numberExclusiveMaximum", new TestRecord(9));
            }).doesNotThrowAnyException();
        }
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