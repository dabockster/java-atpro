// src/test/java/com/atproto/codegen/ClientGeneratorTest.java

package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atproto.api.AtpResponse;
import com.atproto.api.xrpc.XRPCException;
import com.atproto.lexicon.models.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    public void testGenerateClientForSimpleQuery() throws Exception { // Added Exception
        // Test simple Client
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon(); // Create Lexicon.
        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        // Basic checks
        assertTrue(generatedCode.contains("package com.example;")); // Package name
        assertTrue(generatedCode.contains("public class SimpleQueryClient")); // Has class name
        assertTrue(generatedCode.contains("public AtpResponse")); // Returns AtpResponse
        assertTrue(generatedCode.contains("simpleQuery(")); // Method name (same as ID)
        assertTrue(generatedCode.contains(
                "xrpcClient.sendQuery")); // Use XRPC.
        assertFalse(generatedCode.contains(
                "import com.atproto.api.xrpc.XRPCException;")); // Doesn't import XRPC

        // --- Compilation and Reflection ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.SimpleQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

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
        assertTrue(generatedCode.contains("paramsQuery(ParamsQueryParams params")); // Query method with parameters and
                                                                                    // type
        assertTrue(generatedCode.contains("xrpcClient.sendQuery")); // Use XRPC

        // --- Compilation and Reflection ---
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.ParamsQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

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
    public void testXRPCException() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon(); // Create Lexicon. (doesn't really matter which
                                                                      // Lexicon
        // we make, just need one)

        ClientGenerator generator = new ClientGenerator(); // ClientGenerator instance
        String generatedCode = generator.generateClient(lexiconDoc); // Generate

        assertTrue(generatedCode.contains(
                "import com.atproto.api.xrpc.XRPCException;")); // Should import exception
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
                .replace("java.util.concurrent.CompletableFuture", "AtpResponse");
        assertEquals(expectedReturnType, actualReturnType);
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
            if (lexArray.getItems() instanceof LexPrimitive) { // Only supporting primitive returns for now
                return "java.util.List<" + getExpectedPrimitiveType((LexPrimitive) lexArray.getItems()) + ">";
            }

        } else if (schema.getSchema().get() instanceof LexPrimitive) {
            return getExpectedPrimitiveType((LexPrimitive) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexXrpcBody) {
            return getExpectedJavaType((LexXrpcBody) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexRef) {
            return "Object";
        } else if (schema.getSchema().get() instanceof LexString) {
            LexString lexString = (LexString) schema.getSchema().get();
            if (lexString.getFormat().isPresent()) {
                if (lexString.getFormat().equals("datetime")) {
                    return "java.time.Instant";
                } else if (lexString.getFormat().equals("cid")) {
                    return "com.atproto.common.Cid";
                } else if (lexString.getFormat().equals("did")) {
                    return "com.atproto.syntax.Did";
                }
            }
            return "String";
        } else if (schema.getSchema().get() instanceof LexRefUnion) {
            return "Object";
        }

        return "Object"; // Unreachable? Error?
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
    public void testGenerateClientForNestedObject() throws IOException, ClassNotFoundException, NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException, java.lang.reflect.InvocationTargetException {
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

        // 3. Get and Invoke Method, Assert Return Type
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

    @Test
    public void testGenerateClientForRefUnionParams() throws Exception { // Added Exception
        LexiconDoc lexiconDoc = TestUtils.createLexiconWithRefUnionParams();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertTrue(generatedCode.contains("package com.example;"));
        assertTrue(generatedCode.contains("public class RefUnionParamsClient"));
        // Check that the generated code includes the expected parameter and return type
        // (adjust as per codegen)
        // NOTE: This will likely need changing based on how you generate method
        // signatures.
        assertTrue(generatedCode.contains("public AtpResponse refUnionParams(Object params)")); // Object because it
                                                                                                // could be either one
                                                                                                // of the Record Types.

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

    // Data Providers
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
        arrayParams.put("intArrayParam",
                new LexArray(new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
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
        stringFormatParams.put("atUriParam", new LexString(Optional.of("at-uri"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        stringFormatParams.put("cidParam", new LexString(Optional.of("cid"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        stringFormatParams.put("didParam", new LexString(Optional.of("did"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        stringFormatParams.put("handleParam", new LexString(Optional.of("handle"), Optional.empty(), Optional.empty(),
                Optional.empty, Optional.empty()));
        stringFormatParams.put("nsidParam", new LexString(Optional.of("nsid"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        stringFormatParams.put("datetimeParam", new LexString(Optional.of("datetime"), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));
        stringFormatParams.put("languageParam", new LexString(Optional.of("language"), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));

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

        return argList.stream();
    }

    private static Stream<Arguments> provideLexiconsForInvalidLexicons() {
        return Stream.of(
                // Missing 'defs'
                Arguments.of(TestUtils.createLexiconWithoutDefs(), IllegalArgumentException.class),
                // Invalid type within parameters
                Arguments.of(TestUtils.createLexiconWithInvalidType(), IllegalArgumentException.class)

        );

    }
}