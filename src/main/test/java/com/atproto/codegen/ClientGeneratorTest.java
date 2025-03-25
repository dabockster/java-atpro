// src/main/test/java/com/atproto/codegen/ClientGeneratorTest.java
package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atproto.api.AtpResponse;
import com.atproto.api.xrpc.XRPCException;
import com.atproto.lexicon.models.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class ClientGeneratorTest {

    @Mock
    private XrpcClient mockXrpcClient;

    private ClientGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new ClientGenerator();
    }

    @Test
    public void testGenerateClientForSimpleQuery() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertTrue(generatedCode.contains("package com.example;"));
        assertTrue(generatedCode.contains("public class SimpleQueryClient"));
        assertTrue(generatedCode.contains("public AtpResponse"));
        assertTrue(generatedCode.contains("simpleQuery("));
        assertTrue(generatedCode.contains(
                "xrpcClient.sendQuery"));
        assertFalse(generatedCode.contains(
                "import com.atproto.api.xrpc.XRPCException;"));

        // --- Compilation and Reflection ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.SimpleQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Stub the mockXrpcClient to return a successful response
        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

        // Invoke and check return type.
        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");
        Object result = method.invoke(clientInstance);
        assertInstanceOf(AtpResponse.class, result);

    }

    @Test
    public void testGenerateClientForQueryWithParams() throws Exception { // Added Exception
        // Test params Client
        LexiconDoc lexiconDoc = TestUtils.createQueryWithParamsLexicon(); // Create Lexicon
        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Basic checks
        assertTrue(generatedCode.contains("package com.example;")); // Package name
        assertTrue(generatedCode.contains("public class ParamsQueryClient")); // Class Name.
        assertTrue(generatedCode.contains("public AtpResponse")); // Returns AtpResponse
        assertTrue(generatedCode.contains(
                "paramsQuery(ParamsQueryParams params")); // Query method with parameters and type
        assertTrue(generatedCode.contains("xrpcClient.sendQuery")); // Use XRPC

        // --- Compilation and Reflection ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.ParamsQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Stub
        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

        Class<?> paramClass = Class.forName("com.example.ParamsQueryParams");
        Object paramInstance = paramClass.getDeclaredConstructor().newInstance();

        // Invoke and check return type
        java.lang.reflect.Method method = generatedClientClass.getMethod("paramsQuery", paramClass);
        Object result = method.invoke(clientInstance, paramInstance); // Pass the parameter instance
        assertInstanceOf(AtpResponse.class, result);

    }

    @Test
    public void testGenerateClientForProcedure() throws Exception { // Added Exception
        // Test Procedure
        LexiconDoc lexiconDoc = TestUtils.createProcedureLexicon(); // Create Lexicon
        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Basic checks
        assertTrue(generatedCode.contains("package com.example;")); // Package name
        assertTrue(generatedCode.contains("public class ProcedureClient")); // Class name
        assertTrue(generatedCode.contains("public AtpResponse")); // Return AtpResponse
        assertTrue(generatedCode.contains("procedure(ProcedureProcedureInput input")); // Proper params
        assertTrue(generatedCode.contains(
                "xrpcClient.sendProcedure")); // Check for XRPC call (should be checked in its own test)

        // --- Compilation and Reflection ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.ProcedureClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Stub
        when(mockXrpcClient.sendProcedure(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

        Class<?> inputClass = Class.forName("com.example.ProcedureProcedureInput");
        Object inputInstance = inputClass.getDeclaredConstructor().newInstance();

        // Execute and test
        java.lang.reflect.Method method = generatedClientClass.getMethod("procedure", inputClass); // Get method
                                                                                                   // signature
        Object result = method.invoke(clientInstance, inputInstance); // Invoke with parameter
        assertInstanceOf(AtpResponse.class, result);
    }

    @Test
    public void testGenerateClientForSubscription() throws IOException {
        // Test Subscription
        LexiconDoc lexiconDoc = TestUtils.createSubscriptionLexicon(); // Create Lexicon
        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Basic checks (subscriptions might have a very different structure)
        assertTrue(generatedCode.contains("package com.example;")); // Package name
        assertTrue(generatedCode.contains(
                "public class SubscriptionClient")); // Class exists (name should automatically be generated from the
                                                     // name)
        assertTrue(generatedCode.contains("public void")); // void return type.
        assertTrue(
                generatedCode.contains("subscription(")); // Subscription Method. (name should automatically be
                                                          // generated
        // from the name)
        assertTrue(generatedCode.contains(
                "throw new UnsupportedOperationException")); // Subscription not implemented
    }

    @Test
    public void testGenerateClientWithMultipleMethods() throws IOException {
        // Multiple Definitions
        LexiconDoc lexiconDoc = TestUtils.createMultiMethodLexicon(); // Create Lexicon
        ClientGenerator generator = new ClientGenerator(); // ClientGenerator Instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Check for multiple methods
        assertTrue(generatedCode.contains("queryMethod(")); // Query exists
        assertTrue(generatedCode.contains("procedureMethod(")); // Procedure Exists
    }

    // Test for duplicate method names. Should de-dupe
    @Test
    public void testGenerateClientWithDuplicateMethods() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createDuplicateMethodLexicon();
        ClientGenerator generator = new ClientGenerator();

        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Check for multiple methods
        assertEquals(1, countOccurrences(generatedCode, "queryMethod")); // Query exists

    }

    // Helper Method. (Need to find something more formal; maybe write one of my
    // own)
    public int countOccurrences(String text, String word) {

        int count = 0; // Initialize a counter variable

        int fromIndex = 0; // Start from the beginning

        while ((fromIndex = text.indexOf(word, fromIndex)) != -1) {
            count++;
            fromIndex++; // Move past the word
        }

        return count;
    }

    // Test that XRPC Exception gets thrown.
    @Test
    public void testXRPCException() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertTrue(generatedCode.contains("import com.atproto.api.xrpc.XRPCException;"));

        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.SimpleQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Stub the mock to throw the exception
        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any())).thenThrow(new XRPCException(null, null));

        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");

        // Assert that the method call throws the expected exception
        assertThrows(XRPCException.class, () -> {
            method.invoke(clientInstance);
        });
    }

    // Test that AtpResponse gets imported.
    @Test
    public void testAtpResponseType() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon(); // Create Lexicon. (doesn't really matter which
                                                                      // Lexicon
        // we make, just need one)

        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        assertTrue(generatedCode.contains(
                "import com.atproto.api.AtpResponse;")); // Should import AtpResponse
    }

    // Helper methods for creating Lexicon structures (for test readability)
    // These would ideally be in a separate test utility class.

    @ParameterizedTest
    @MethodSource("provideLexiconsForAllParameterTypes")
    public void testGenerateClientForVariousParameterTypes(LexiconDoc lexiconDoc, String paramName, String expectedType)
            throws Exception { // Added Exception
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // General checks (package, class name, etc.)
        assertTrue(generatedCode.contains("package com.example;"));

        // More specific checks based on parameter type. This is where we use the
        // paramName.
        assertTrue(generatedCode.contains(paramName)); // Very basic, does the parameter exist?

        // Check that parameter type is correct
        assertTrue(generatedCode.contains(expectedType + " " + paramName));

        // --- Compilation and Reflection to check return type ---
        String className = lexiconDoc.getId().substring(lexiconDoc.getId().lastIndexOf('.') + 1) + "Client";
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example." + className, generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // Inject mockXrpcClient
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Find the method and its return type. This assumes the method name is "main"
        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method m : generatedClientClass.getMethods()) {
            if (m.getName().equals("main")) { // Or whatever the method name is in your generated code
                method = m;
                break;
            }
        }
        assertNotNull(method, "Method 'main' not found in generated class");

        // Get Return Type from Lexicon, create expected type from this
        String expectedReturnType = "AtpResponse"; // Default
        if (lexiconDoc.getDefs().get("main") instanceof LexXrpcQuery) {
            LexXrpcQuery query = (LexXrpcQuery) lexiconDoc.getDefs().get("main");
            if (query.getOutput().isPresent() && query.getOutput().get().getSchema().isPresent()) {
                expectedReturnType = "AtpResponse<" + getExpectedJavaType((LexXrpcBody) query.getOutput().get()) + ">";
            } else {
                expectedReturnType = "AtpResponse<Void>";
            }
        } else if (lexiconDoc.getDefs().get("main") instanceof LexXrpcProcedure) {
            LexXrpcProcedure procedure = (LexXrpcProcedure) lexiconDoc.getDefs().get("main");
            if (procedure.getOutput().isPresent() && procedure.getOutput().get().getSchema().isPresent()) {
                expectedReturnType = "AtpResponse<" + getExpectedJavaType((LexXrpcBody) procedure.getOutput().get())
                        + ">";

            } else {
                expectedReturnType = "AtpResponse<Void>";
            }
        } else if (lexiconDoc.getDefs().get("main") instanceof LexXrpcSubscription) {
            expectedReturnType = "void";
        }

        // Now we assert that the return type from code gen is as expected
        String actualReturnType = method.getGenericReturnType().getTypeName()
                .replace("java.util.concurrent.CompletableFuture", "AtpResponse"); // Simplify for testing
        assertEquals(expectedReturnType, actualReturnType);

        // Stub Mockito for the method call (added)
        if (method.getParameterCount() > 0) { // For parameters
            when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                    .thenReturn(new AtpResponse<>(null, Optional.empty()));
        } else { // No Parameters
            when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                    .thenReturn(new AtpResponse<>(null, Optional.empty()));
        }

    }

    // Helper function to return expected Java type String from Lexicon definition.
    private String getExpectedJavaType(LexXrpcBody xrpcBody) {

        if (xrpcBody.getSchema().isEmpty()) {
            return "Void";
        }

        LexXrpcBody schema = xrpcBody;
        if (schema.getSchema().get() instanceof LexObject) {
            return "Object"; // Placeholder, replace with generated class name if using nested objects.
        } else if (schema.getSchema().get() instanceof LexArray) {
            LexArray lexArray = (LexArray) schema.getSchema().get();
            // Handle nested arrays
            return getExpectedArrayType(lexArray);
        } else if (schema.getSchema().get() instanceof LexPrimitive) {
            return getExpectedPrimitiveType((LexPrimitive) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexXrpcBody) {
            return getExpectedJavaType((LexXrpcBody) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexRef) {
            // Resolve references, including repository refs vs. defs refs. This is very
            // basic.
            LexRef ref = (LexRef) schema.getSchema().get();
            String refStr = ref.getRef();
            if (refStr.startsWith("#")) {
                return refStr.substring(refStr.lastIndexOf(".") + 1); // Local ref
            } else {
                return refStr.replace(".", ""); // External ref
            }
        } else if (schema.getSchema().get() instanceof LexString) {
            LexString lexString = (LexString) schema.getSchema().get();
            if (lexString.getFormat().isPresent()) {
                if (lexString.getFormat().get().equals("datetime")) {
                    return "java.time.Instant";
                } else if (lexString.getFormat().get().equals("cid")) {
                    return "com.atproto.common.Cid";
                } else if (lexString.getFormat().get().equals("did")) {
                    return "com.atproto.syntax.Did";
                } else if (lexString.getFormat().get().equals("handle")) {
                    return "com.atproto.syntax.Handle";
                } else if (lexString.getFormat().get().equals("at-uri")) {
                    return "com.atproto.syntax.AtUri";
                } else if (lexString.getFormat().get().equals("nsid")) {
                    return "com.atproto.syntax.Nsid";
                } else if (lexString.getFormat().get().equals("uri")) {
                    return "java.net.URI";
                } else if (lexString.getFormat().get().equals("language")) {
                    return "java.util.Locale";
                } else if (lexString.getFormat().get().equals("uri-reference")) {
                    return "java.net.URI"; // Assuming URI for uri-reference
                } else if (lexString.getFormat().get().equals("uri-template")) {
                    return "java.lang.String"; // Assuming String for uri-template (no built-in type)
                } else if (lexString.getFormat().get().equals("email")) {
                    return "java.lang.String"; // Assuming String for email
                } else if (lexString.getFormat().get().equals("hostname")) {
                    return "java.lang.String"; // Assuming String for hostname
                } else if (lexString.getFormat().get().equals("ipv4")) {
                    return "java.net.InetAddress"; // Assuming InetAddress for IPv4
                } else if (lexString.getFormat().get().equals("ipv6")) {
                    return "java.net.InetAddress"; // Assuming InetAddress for IPv6

                }
            }
            return "String";
        } else if (schema.getSchema().get() instanceof LexRefUnion) {
            return "Object";
        }

        return "Object"; // Unreachable? Error?
    }

    private String getExpectedArrayType(LexArray lexArray) {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.List<");
        LexType itemType = lexArray.getItems();

        if (itemType instanceof LexPrimitive) {
            sb.append(getExpectedPrimitiveType((LexPrimitive) itemType));
        } else if (itemType instanceof LexArray) {
            sb.append(getExpectedArrayType((LexArray) itemType)); // Recurse for nested arrays
        } else if (itemType instanceof LexRef) {
            // Resolve references
            LexRef ref = (LexRef) itemType;
            String refStr = ref.getRef();
            if (refStr.startsWith("#")) {
                sb.append(refStr.substring(refStr.lastIndexOf(".") + 1));
            } else {
                sb.append(refStr.replace(".", ""));
            }
        } else if (itemType instanceof LexRefUnion) {
            sb.append("Object"); // No specific type in a refUnion
        } else if (itemType instanceof LexObject) { // Nested object not allowed as Array
            return "Object";
        }

        sb.append(">");
        return sb.toString();
    }

    private String getExpectedPrimitiveType(LexPrimitive prim) {
        if (prim instanceof LexBoolean) {
            return "Boolean";
        } else if (prim instanceof LexInteger) {
            return "Integer";
        } else if (prim instanceof LexString) {
            return "String";
        } else if (prim instanceof LexNumber) {
            return "Float";
        } else if (prim instanceof LexBytes) {
            return "byte[]";
        } else if (prim instanceof LexUnknown) {
            return "java.util.Map<String, Object>";
        }
        return "Object"; // Should not happen in valid Lexicon
    }

    @Test
    public void testGenerateClientForNestedObject() throws Exception { // Added Exception
        LexiconDoc lexiconDoc = TestUtils.createNestedObjectLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // Basic checks
        assertTrue(generatedCode.contains("package com.example;")); // Package name
        assertTrue(generatedCode.contains("public class NestedObjectClient")); // Class Name.
        assertTrue(generatedCode.contains("public AtpResponse")); // Returns AtpResponse
        assertTrue(generatedCode.contains(
                "nestedObject(")); // Query method with parameters and type

        // --- Compilation and Reflection, as before, BUT: ---
        // 1. Compile and Get Class:
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.NestedObjectClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // 2. Inject Mock:
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // 3. Stub Mockito
        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

        // 4. Get and Invoke Method, Assert Return Type
        java.lang.reflect.Method method = generatedClientClass.getMethod("nestedObject");
        Object result = method.invoke(clientInstance);
        assertInstanceOf(AtpResponse.class, result); // Very basic check

        // You could add in more specific checks with Mockito here to check call
        // parameters
    }

    @ParameterizedTest
    @MethodSource("provideLexiconsForInvalidLexicons")
    public void testInvalidLexiconStructure(LexiconDoc lexiconDoc, Class<? extends Exception> expectedException) {
        ClientGenerator generator = new ClientGenerator();
        assertThrows(expectedException, () -> generator.generateClient(lexiconDoc));
    }

    @Test
    public void testMultipleDefs() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createLexiconWithMultipleDefs();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);
        assertTrue(generatedCode.contains("class Query1Client"));
        assertTrue(generatedCode.contains("class Query2Client"));
        assertTrue(generatedCode.contains("class Record1Record")); // Check for records.

    }

    @ParameterizedTest // Enhanced to be parameterized
    @MethodSource("provideLexiconsForRefUnionParams")
    public void testGenerateClientForRefUnionParams(LexiconDoc lexiconDoc, String methodName, String expectedParamType)
            throws Exception {
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertTrue(generatedCode.contains("package com.example;"));
        String className = lexiconDoc.getId().substring(lexiconDoc.getId().lastIndexOf('.') + 1) + "Client";

        assertTrue(generatedCode.contains("public class " + className));

        // Dynamic method name check
        assertTrue(generatedCode.contains("public AtpResponse " + methodName + "(" + expectedParamType + " params)"));

        // --- Compilation, Reflection, and Mockito Stubbing ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example." + className, generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
        Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // Find the method dynamically
        java.lang.reflect.Method method = generatedClientClass.getMethod(methodName, Class.forName(expectedParamType));
        assertNotNull(method, "Method '" + methodName + "' not found in generated class");

        // Basic stubbing (you'd expand this for specific test cases)
        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

    }

    @Test
    public void testJavadocGeneration() throws Exception { // Added Exception
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexiconWithDescription();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // Basic Javadoc checks
        assertTrue(generatedCode.contains("/**"));
        assertTrue(generatedCode.contains("* This is a test query.")); // Check for the description.
        assertTrue(generatedCode.contains("*/"));
        assertTrue(generatedCode.contains("public class SimpleQueryClientWithDescription"));

        // Compile and load the class, then verify with reflection
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler
                .compile("com.example.SimpleQueryClientWithDescription", generatedCode);
        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");
        assertNotNull(method.getAnnotations(), "Method should have annotations"); // A very basic check. Could be more
                                                                                  // thorough.

    }

    @ParameterizedTest
    @MethodSource("provideLexiconsForStringConstraints")
    public void testStringConstraints(LexiconDoc lexiconDoc, String paramName, String expectedType,
            Integer maxLength, Integer minLength, String constValue, String pattern) throws Exception {
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // General checks, compilation, method retrieval (as before)
        String className = lexiconDoc.getId().substring(lexiconDoc.getId().lastIndexOf('.') + 1) + "Client";
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example." + className, generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method m : generatedClientClass.getMethods()) {
            if (m.getName().equals("main")) {
                method = m;
                break;
            }
        }
        Class<?> paramClass = method.getParameterTypes()[0];
        // Now using reflection
        java.lang.reflect.Field paramField = null;
        try {
            paramField = paramClass.getDeclaredField(paramName);

        } catch (Exception e) {

            fail("parameter not found in parameters of generated class.");
        }

        // maxLength constraint
        if (maxLength != null) {
            Annotation maxLengthAnnotation = paramField.getAnnotation(MaxLength.class);
            assertNotNull(maxLengthAnnotation, "MaxLength annotation not found");
            assertEquals(maxLength.intValue(), ((MaxLength) maxLengthAnnotation).value(),
                    "MaxLength value mismatch");
        }

        // minLength constraint
        if (minLength != null) {
            Annotation minLengthAnnotation = paramField.getAnnotation(MinLength.class);
            assertNotNull(minLengthAnnotation, "MinLength annotation not found");
            assertEquals(minLength.intValue(), ((MinLength) minLengthAnnotation).value(),
                    "MinLength value mismatch");
        }

        // const constraint
        if (constValue != null) {
            assertTrue(generatedCode
                    .contains("public static final String " + paramName.UPPER_CASE() + " = \"" + constValue + "\";"));
        }

        // Regex pattern (using annotation value)
        if (pattern != null) {
            Annotation patternAnnotation = paramField.getAnnotation(Pattern.class);
            assertNotNull(patternAnnotation, "Pattern annotation not found.");
            assertEquals(pattern, ((Pattern) patternAnnotation).regexp(), "Pattern regexp() value mismatch.");
        }

        // Test constraint violations by *attempting* to set bad values.
        Object paramInstance = paramClass.getDeclaredConstructor().newInstance();

        // Test maxLength violation
        if (maxLength != null) {
            String longString = "a".repeat(maxLength + 1);
            assertThrows(ConstraintViolationException.class, () -> {
                paramField.setAccessible(true); // Make it accessible.
                paramField.set(paramInstance, longString); // Set the parameter, throw if invalid.
            });

        }

        // Test minLength violation
        if (minLength != null) {
            String shortString = "a".repeat(minLength - 1);
            assertThrows(ConstraintViolationException.class, () -> {
                paramField.setAccessible(true); // Make it accessible.
                paramField.set(paramInstance, shortString); // Set the parameter, throw if invalid.
            });
        }

        // Test const violation (we set to the valid, and then an invalid value)

        if (constValue != null) {
            String validValue = constValue;
            paramField.setAccessible(true); // Make field accessible.
            paramField.set(paramInstance, validValue); // Set GOOD.
            String invalidValue = validValue + "_INVALID";

            assertThrows(ConstraintViolationException.class, () -> {
                paramField.setAccessible(true); // Make it accessible.
                paramField.set(paramInstance, invalidValue); // Try set invalid value.
            });
        }

        // Test pattern violation
        if (pattern != null) {
            // Try setting invalid input.
            assertThrows(ConstraintViolationException.class, () -> {
                paramField.setAccessible(true); // Make it accessible.
                paramField.set(paramInstance, "123"); // Try set invalid numerical value.
            });
        }

    }

    // Data Providers (Continued)

    // Parameterized Test for Ref Union Params: Now provides Lexicons, Method Name,
    // and expected Parameter Type.
    private static Stream<Arguments> provideLexiconsForRefUnionParams() {
        return Stream.of(
                Arguments.of(TestUtils.createLexiconWithRefUnionParams(), "refUnionParams", "java.lang.Object") // Object
                                                                                                                // for
                                                                                                                // now,
                                                                                                                // may
                                                                                                                // be
                                                                                                                // refined
        );
    }

    // Added Invalid Lex Version Test
    @ParameterizedTest
    @MethodSource("provideLexiconsForInvalidLexVersions")
    public void testInvalidLexiconVersion(LexiconDoc lexiconDoc, Class<? extends Exception> expectedException) {
        ClientGenerator generator = new ClientGenerator();
        assertThrows(expectedException, () -> generator.generateClient(lexiconDoc)); // Expect an exception
    }

    private static Stream<Arguments> provideLexiconsForInvalidLexVersions() {
        List<LexDefinition> defs = new ArrayList<>();
        LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
        LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.of("This is a test query."), Optional.empty(),
                Optional.of(output), new ArrayList<>()); // Added description
        defs.add(new LexDefinition("main", "query", query));

        return Stream.of(
                // lex value of 0 is invalid
                Arguments.of(new LexiconDoc(0, "com.example.invalidVersion", Optional.of(0), Optional.empty(),
                        defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                java.util.function.Function.identity()))),
                        IllegalArgumentException.class) // Invalid Version

        );
    }

    // Added Valid Lex Version test
    @ParameterizedTest
    @MethodSource("provideLexiconsForValidLexVersions")
    public void testValidLexiconVersion(LexiconDoc lexiconDoc) throws Exception {
        // Valid Lex Version
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // --- Compilation and Reflection ---
        String className = lexiconDoc.getId().substring(lexiconDoc.getId().lastIndexOf('.') + 1) + "Client";
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example." + className,
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // Inject mockXrpcClient
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenReturn(new AtpResponse<>(null, Optional.empty()));

    }

    private static Stream<Arguments> provideLexiconsForValidLexVersions() {
        List<LexDefinition> defs = new ArrayList<>();
        LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
        LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.of("This is a test query."), Optional.empty(),
                Optional.of(output), new ArrayList<>()); // Added description
        defs.add(new LexDefinition("main", "query", query));

        return Stream.of(
                // lex value of 1
                Arguments.of(new LexiconDoc(1, "com.example.validversion", Optional.of(0), Optional.empty(),
                        defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                java.util.function.Function.identity()))))

        );
    }

    private static Stream<Arguments> provideLexiconsForAllParameterTypes() {
        List<Arguments> argList = new ArrayList<>();

        // Integer types
        Map<String, LexPrimitive> intParams = new HashMap<>();
        intParams.put("intParam",
                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.intParams", intParams), "intParam",
                "Integer"));

        // Number types (float/double) part of LexNumber
        Map<String, LexPrimitive> numberParams = new HashMap<>();
        numberParams.put("floatParam",
                new LexNumber(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.floatParams", numberParams),
                "floatParam", "Float")); // Double, double

        // String types
        Map<String, LexPrimitive> stringParams = new HashMap<>();
        stringParams.put("stringParam", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringParams", stringParams),
                "stringParam", "String"));

        // Boolean types
        Map<String, LexPrimitive> boolParams = new HashMap<>();
        boolParams.put("boolParam", new LexBoolean(Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.boolParams", boolParams), "boolParam",
                "Boolean"));

        // Bytes type
        Map<String, LexPrimitive> bytesParams = new HashMap<>();
        bytesParams.put("bytesParam",
                new LexBytes(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.bytesParams", bytesParams),
                "bytesParam", "byte[]"));

        // CidLink
        Map<String, LexPrimitive> cidLinkParams = new HashMap<>();
        cidLinkParams.put("cidLinkParam", new LexCidLink(Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.cidLinkParams", cidLinkParams),
                "cidLinkParam", "com.atproto.common.Cid"));

        // Array of primitives
        Map<String, LexType> arrayParams = new HashMap<>();
        arrayParams.put("intArrayParam", new LexArray(
                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.arrayParams", arrayParams),
                "intArrayParam", "java.util.List<Integer>"));

        // Unknown
        Map<String, LexPrimitive> unknownParams = new HashMap<>();
        unknownParams.put("unknownParam", new LexUnknown(Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.unknownParams", unknownParams),
                "unknownParam", "java.util.Map<String, Object>"));

        // String Formats.
        Map<String, LexPrimitive> stringFormatParams = new HashMap<>();
        stringFormatParams.put("atUriParam", new LexString(Optional.of("at-uri"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("cidParam", new LexString(Optional.of("cid"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("didParam", new LexString(Optional.of("did"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("handleParam", new LexString(Optional.of("handle"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("nsidParam", new LexString(Optional.of("nsid"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("datetimeParam", new LexString(Optional.of("datetime"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("languageParam", new LexString(Optional.of("language"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("uriParam", new LexString(Optional.of("uri"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("uriRefParam", new LexString(Optional.of("uri-reference"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("uriTemplateParam", new LexString(Optional.of("uri-template"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("emailParam", new LexString(Optional.of("email"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("hostnameParam", new LexString(Optional.of("hostname"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("ipv4Param", new LexString(Optional.of("ipv4"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("ipv6Param", new LexString(Optional.of("ipv6"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "atUriParam", "com.atproto.syntax.AtUri"));
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "cidParam", "com.atproto.common.Cid")); // Assuming you have a Cid class.

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "didParam", "com.atproto.syntax.Did")); // Assuming you have a Did class

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "handleParam", "com.atproto.syntax.Handle")); // Assuming you have a Handle class

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "nsidParam", "com.atproto.syntax.Nsid")); // Assuming you have an NSID class.

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "datetimeParam", "java.time.Instant"));

        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "languageParam", "java.util.Locale"));
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "uriParam", "java.net.URI"));
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "uriRefParam", "java.net.URI")); // Assuming URI for uri-reference
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "uriTemplateParam", "java.lang.String")); // Assuming String for uri-template (no built-in type)
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "emailParam", "java.lang.String")); // Assuming String for email
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "hostnameParam", "java.lang.String")); // Assuming String for hostname
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "ipv4Param", "java.net.InetAddress")); // Assuming InetAddress for IPv4
        argList.add(
                Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams", stringFormatParams),
                        "ipv6Param", "java.net.InetAddress")); // Assuming InetAddress for IPv6
        return argList.stream();
    }

    private static Stream<Arguments> provideLexiconsForStringConstraints() {
        return Stream.of(
                Arguments.of(createLexiconWithStringConstraints(), "maxLengthString", "String", 10, null, null, null),
                Arguments.of(createLexiconWithStringConstraints(), "minLengthString", "String", null, 5, null, null),
                Arguments.of(createLexiconWithStringConstraints(), "constString", "String", null, null, "constantValue",
                        null),
                Arguments.of(createLexiconWithStringConstraints(), "patternString", "String", null, null, null,
                        "[a-zA-Z]+"));
    }

}
