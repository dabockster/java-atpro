// src/main/test/java/com/atproto/codegen/ClientGeneratorTest.java
package com.atproto.codegen;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.regex.Pattern;
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
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class ClientGeneratorTest {

    @Mock
    private XrpcClient mockXrpcClient;

    @InjectMocks
    private ClientGenerator generator;

    @BeforeEach
    public void setUp() {
        // Reset mocks between tests
        Mockito.reset(mockXrpcClient);
    }

    @ParameterizedTest
    @MethodSource("provideImportData")
    void testGeneratedImports(String generatedCode, String[] expectedImports) {
        for (String expectedImport : expectedImports) {
            assertThat(generatedCode)
                .contains("import " + expectedImport + ";")
                .as("Expected import not found: " + expectedImport);
        }
    }

    private static Stream<Arguments> provideImportData() {
        return Stream.of(
            Arguments.of("", new String[] {}),
            Arguments.of("import java.util.List;", new String[] {"java.util.List"})
        );
    }

    private void verifyImports(String generatedCode, String... expectedImports) {
        for (String expectedImport : expectedImports) {
            assertThat(generatedCode)
                .contains("import " + expectedImport + ";")
                .as("Expected import not found: " + expectedImport);
        }
    }

    @Test
    public void testGenerateClientForSimpleQuery() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertThat(generatedCode).contains("package com.example;");
        assertThat(generatedCode).contains("public class SimpleQueryClient");
        assertThat(generatedCode).contains("public AtpResponse");
        assertThat(generatedCode).contains("simpleQuery(");
        assertThat(generatedCode).contains("xrpcClient.sendQuery");
        assertThat(generatedCode).doesNotContain("import com.atproto.api.xrpc.XRPCException;"); // No params, no XRPCException
        verifyImports(generatedCode, "com.atproto.api.AtpResponse");

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
        assertThat(result).isInstanceOf(AtpResponse.class);
    }

    @Test
    public void testGenerateClientForQueryWithParams() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createQueryWithParamsLexicon();
        String generatedCode = generator.generateClient(lexiconDoc);

        // Basic checks
        assertThat(generatedCode).contains("package com.example;");
        assertThat(generatedCode).contains("public class ParamsQueryClient");
        assertThat(generatedCode).contains("public AtpResponse");
        assertThat(generatedCode).contains("paramsQuery(ParamsQueryParams params");
        assertThat(generatedCode).contains("xrpcClient.sendQuery");

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
        Object result = method.invoke(clientInstance, paramInstance);
        assertThat(result).isInstanceOf(AtpResponse.class);

        verifyImports(generatedCode,
                "com.example.ParamsQueryParams",
                "com.atproto.api.AtpResponse", "java.util.Optional");
    }

    @Test
    public void testGenerateClientForProcedure() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createProcedureLexicon();
        String generatedCode = generator.generateClient(lexiconDoc);

        // Basic checks
        assertThat(generatedCode).contains("package com.example;");
        assertThat(generatedCode).contains("public class ProcedureClient");
        assertThat(generatedCode).contains("public AtpResponse");
        assertThat(generatedCode).contains("procedure(ProcedureProcedureInput input");
        assertThat(generatedCode).contains("xrpcClient.sendProcedure");

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
        java.lang.reflect.Method method = generatedClientClass.getMethod("procedure", inputClass);
        Object result = method.invoke(clientInstance, inputInstance);
        assertThat(result).isInstanceOf(AtpResponse.class);
        verifyImports(generatedCode, "com.example.ProcedureProcedureInput", "com.atproto.api.AtpResponse",
                "java.util.Optional");
    }

    @Test
    public void testGenerateClientForSubscription() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createSubscriptionLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertThat(generatedCode).contains("package com.example;");
        assertThat(generatedCode).contains("public class SubscriptionClient");
        assertThat(generatedCode).contains("public void");
        assertThat(generatedCode).contains("subscription(");
        assertThat(generatedCode).contains("throw new UnsupportedOperationException");

        // Subscriptions typically don't have explicit input/output, so minimal imports
        // are expected.
        // We still check for the package declaration. No explicit import check here.
    }

    @Test
    public void testGenerateClientWithMultipleMethods() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createMultiMethodLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertThat(generatedCode).contains("queryMethod(");
        assertThat(generatedCode).contains("procedureMethod(");
        verifyImports(generatedCode, "com.atproto.api.AtpResponse"); // At least AtpResponse should be there

    }

    @Test
    public void testGenerateClientWithDuplicateMethods() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createDuplicateMethodLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);
        int count = countOccurrences(generatedCode, "queryMethod");
        assertThat(count).isEqualTo(1);
    }

    public int countOccurrences(String text, String word) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(word, fromIndex)) != -1) {
            count++;
            fromIndex++;
        }
        return count;
    }

    @Test
    public void testXRPCException() throws Exception {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        // This specific test CHECKS FOR THE *PRESENCE* of the XRPCException import.
        // The original test expected it to be absent *because* it was a query with NO
        // parameters.
        // But XRPCException can still be thrown by the underlying sendQuery, even
        // without parameters.
        assertThat(generatedCode).contains("import com.atproto.api.xrpc.XRPCException;");

        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example.SimpleQueryClient",
                generatedCode);
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                .thenThrow(new XRPCException(null, null));

        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");

        assertThrows(XRPCException.class, () -> method.invoke(clientInstance));
    }

    @Test
    public void testAtpResponseType() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);
        assertThat(generatedCode).contains("import com.atproto.api.AtpResponse;"); // Should import AtpResponse
    }

    @ParameterizedTest
    @MethodSource("provideLexiconsForAllParameterTypes")
    public void testGenerateClientForVariousParameterTypes(
            LexiconDoc lexiconDoc, String paramName, String expectedType, String expectedImport)
            throws Exception {
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertThat(generatedCode).contains("package com.example;");
        assertThat(generatedCode).contains(paramName);
        assertThat(generatedCode).contains(expectedType + " " + paramName);

        verifyImports(generatedCode, expectedImport); // Verify the specific import

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
        assertNotNull(method, "Method 'main' not found in generated class");

        String expectedReturnType = "AtpResponse";
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
                expectedReturnType = "AtpResponse<"
                        + getExpectedJavaType((LexXrpcBody) procedure.getOutput().get())
                        + ">";

            } else {
                expectedReturnType = "AtpResponse<Void>";
            }
        } else if (lexiconDoc.getDefs().get("main") instanceof LexXrpcSubscription) {
            expectedReturnType = "void";
        }

        String actualReturnType = method.getGenericReturnType().getTypeName()
                .replace("java.util.concurrent.CompletableFuture", "AtpResponse");
        assertThat(expectedReturnType).isEqualTo(actualReturnType);

        if (method.getParameterCount() > 0) {
            when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                    .thenReturn(new AtpResponse<>(null, Optional.empty()));
        } else {
            when(mockXrpcClient.sendQuery(anyString(), any(), any(), any()))
                    .thenReturn(new AtpResponse<>(null, Optional.empty()));
        }
    }

    private String getExpectedJavaType(LexXrpcBody xrpcBody) {

        if (xrpcBody.getSchema().isEmpty()) {
            return "Void";
        }

        LexXrpcBody schema = xrpcBody;
        if (schema.getSchema().get() instanceof LexObject) {
            return "Object";
        } else if (schema.getSchema().get() instanceof LexArray) {
            LexArray lexArray = (LexArray) schema.getSchema().get();
            return getExpectedArrayType(lexArray);
        } else if (schema.getSchema().get() instanceof LexPrimitive) {
            return getExpectedPrimitiveType((LexPrimitive) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexXrpcBody) {
            return getExpectedJavaType((LexXrpcBody) schema.getSchema().get());
        } else if (schema.getSchema().get() instanceof LexRef) {
            LexRef ref = (LexRef) schema.getSchema().get();
            String refStr = ref.getRef();
            if (refStr.startsWith("#")) {
                return refStr.substring(refStr.lastIndexOf(".") + 1);
            } else {
                return refStr.replace(".", "");
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
                    return "java.net.InetAddress"; // Assuming InetAddress
                } else if (lexString.getFormat().get().equals("ipv6")) {
                    return "java.net.InetAddress"; // Assuming InetAddress for IPv6
                }
            }
            return "String";
        } else if (schema.getSchema().get() instanceof LexRefUnion) {
            return "Object"; // RefUnions are expected to be Object
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
            return "Float"; // Keep float as the default for numbers
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
        assertThat(generatedCode).contains("package com.example;"); // Package name
        assertThat(generatedCode).contains("public class NestedObjectClient"); // Class Name.
        assertThat(generatedCode).contains("public AtpResponse"); // Returns AtpResponse
        assertThat(
                generatedCode).contains(
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
        assertThat(result).isInstanceOf(AtpResponse.class); // Very basic check

        // You could add in more specific checks with Mockito here to check call
        // parameters
    }

    @ParameterizedTest
    @MethodSource("provideLexiconsForInvalidLexicons")
    public void testInvalidLexiconStructure(
            LexiconDoc lexiconDoc, Class<? extends Exception> expectedException) {
        ClientGenerator generator = new ClientGenerator();
        assertThrows(expectedException, () -> generator.generateClient(lexiconDoc));
    }

    @Test
    public void testMultipleDefs() throws IOException {
        LexiconDoc lexiconDoc = TestUtils.createLexiconWithMultipleDefs();
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);
        assertThat(generatedCode).contains("class Query1Client");
        assertThat(generatedCode).contains("class Query2Client");
        assertThat(generatedCode).contains("class Record1Record"); // Check for records.
    }

    @ParameterizedTest // Enhanced to be parameterized
    @MethodSource("provideLexiconsForRefUnionParams")
    public void testGenerateClientForRefUnionParams(
            LexiconDoc lexiconDoc, String methodName, String expectedParamType, String expectedImport)
            throws Exception {
        ClientGenerator generator = new ClientGenerator();
        String generatedCode = generator.generateClient(lexiconDoc);

        assertThat(generatedCode).contains("package com.example;");
        String className = lexiconDoc.getId().substring(lexiconDoc.getId().lastIndexOf('.') + 1) + "Client";

        assertThat(generatedCode).contains("public class " + className);

        // Dynamic method name check
        assertThat(
                generatedCode).contains(
                        "public AtpResponse " + methodName + "(" + expectedParamType + " params)");

        verifyImports(generatedCode, expectedImport);

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
        assertThat(generatedCode).contains("/**");
        assertThat(generatedCode).contains("* This is a test query."); // Check for the description.
        assertThat(generatedCode).contains("*/");
        assertThat(generatedCode).contains("public class SimpleQueryClientWithDescription");

        // Compile and load the class, then verify with reflection
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(
                "com.example.SimpleQueryClientWithDescription", generatedCode);
        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");
        assertNotNull(
                method.getAnnotations(), "Method should have annotations"); // A very basic check. Could be more
        // thorough.
    }

    @ParameterizedTest
    @MethodSource("provideLexiconsForStringConstraints")
    public void testStringConstraints(
            LexiconDoc lexiconDoc,
            String paramName,
            String expectedType,
            Integer maxLength,
            Integer minLength,
            String constValue,
            String pattern)
            throws Exception {
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
            assertEquals(
                    maxLength.intValue(), ((MaxLength) maxLengthAnnotation).value(), "MaxLength value mismatch");
        }

        // minLength constraint
        if (minLength != null) {
            Annotation minLengthAnnotation = paramField.getAnnotation(MinLength.class);
            assertNotNull(minLengthAnnotation, "MinLength annotation not found");
            assertEquals(
                    minLength.intValue(), ((MinLength) minLengthAnnotation).value(), "MinLength value mismatch");
        }

        // const constraint
        if (constValue != null) {
            assertThat(
                    generatedCode).contains(
                            "public static final String "
                                    + paramName.toUpperCase()
                                    + " = \""
                                    + constValue
                                    + "\";");
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
            assertThrows(
                    ConstraintViolationException.class,
                    () -> {
                        paramField.setAccessible(true); // Make it accessible.
                        paramField.set(paramInstance, longString); // Set the parameter, throw if invalid.
                    });
        }

        // Test minLength violation
        if (minLength != null) {
            String shortString = "a".repeat(minLength - 1);
            assertThrows(
                    ConstraintViolationException.class,
                    () -> {
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

            assertThrows(
                    ConstraintViolationException.class,
                    () -> {
                        paramField.setAccessible(true); // Make it accessible.
                        paramField.set(paramInstance, invalidValue); // Try set invalid value.
                    });
        }
        // Test pattern violation
        if (pattern != null) {
            // Try setting invalid input.
            assertThrows(
                    ConstraintViolationException.class,
                    () -> {
                        paramField.setAccessible(true); // Make it accessible.
                        paramField.set(paramInstance, "123"); // Try set invalid numerical value.
                    });
        }
    }

    // Data Providers (Continued)

    private static Stream<Arguments> provideLexiconsForRefUnionParams() {
        return Stream.of(
                Arguments.of(
                        TestUtils.createLexiconWithRefUnionParams(),
                        "refUnionParams",
                        "java.lang.Object",
                        "java.lang.Object" // Expect Object import
                ));
    }

    // Added Invalid Lex Version Test
    @ParameterizedTest
    @MethodSource("provideLexiconsForInvalidLexVersions")
    public void testInvalidLexiconVersion(
            LexiconDoc lexiconDoc, Class<? extends Exception> expectedException) {
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
        Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile("com.example." + className, generatedCode);
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
        LexXrpcQuery query = new LexXrpcQuery(
                Optional.empty(),
                Optional.of("This is a test query."),
                Optional.empty(),
                Optional.of(output),
                new ArrayList<>()); // Added description
        defs.add(new LexDefinition("main", "query", query));

        return Stream.of(
                // lex value of 1
                Arguments.of(
                        new LexiconDoc(
                                1,
                                "com.example.validversion",
                                Optional.of(0),
                                Optional.empty(),
                                defs.stream()
                                        .collect(
                                                java.util.stream.Collectors.toMap(
                                                        LexDefinition::getId,
                                                        java.util.function.Function.identity())))));
    }

    private static Stream<Arguments> provideLexiconsForAllParameterTypes() {
        List<Arguments> argList = new ArrayList<>();

        // Integer types
        Map<String, LexPrimitive> intParams = new HashMap<>();
        intParams.put("intParam",
                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.intParams", intParams), "intParam",
                "Integer", "java.lang.Integer"));

        // Number types (float/double) part of LexNumber
        Map<String, LexPrimitive> numberParams = new HashMap<>();
        numberParams.put("floatParam",
                new LexNumber(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.floatParams", numberParams),
                "floatParam", "Float", "java.lang.Float")); // Double, double

        // String types
        Map<String, LexPrimitive> stringParams = new HashMap<>();
        stringParams.put("stringParam", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringParams", stringParams),
                "stringParam", "String", "java.lang.String"));
        // Boolean types
        Map<String, LexPrimitive> boolParams = new HashMap<>();
        boolParams.put("boolParam", new LexBoolean(Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.boolParams", boolParams),
                "boolParam", "Boolean", "java.lang.Boolean"));
        // Bytes type
        Map<String, LexPrimitive> bytesParams = new HashMap<>();
        bytesParams.put("bytesParam",
                new LexBytes(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.bytesParams", bytesParams),
                "bytesParam", "byte[]", "byte[]"));
        // CidLink
        Map<String, LexPrimitive> cidLinkParams = new HashMap<>();
        cidLinkParams.put("cidLinkParam", new LexCidLink(Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.cidLinkParams", cidLinkParams),
                "cidLinkParam", "com.atproto.common.Cid", "com.atproto.common.Cid"));
        // Array of primitives
        Map<String, LexType> arrayParams = new HashMap<>();
        arrayParams.put("intArrayParam", new LexArray(
                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.arrayParams", arrayParams),
                "intArrayParam", "java.util.List<Integer>", "java.util.List<java.lang.Integer>"));
        // Unknown
        Map<String, LexPrimitive> unknownParams = new HashMap<>();
        unknownParams.put("unknownParam", new LexUnknown(Optional.empty()));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.unknownParams", unknownParams),
                "unknownParam", "java.util.Map<String, Object>", "java.util.Map<java.lang.String, java.lang.Object>"));
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
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "atUriParam", "com.atproto.syntax.AtUri", "com.atproto.syntax.AtUri"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "cidParam", "com.atproto.common.Cid", "com.atproto.common.Cid"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "didParam", "com.atproto.syntax.Did", "com.atproto.syntax.Did"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "handleParam", "com.atproto.syntax.Handle", "com.atproto.syntax.Handle"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "nsidParam", "com.atproto.syntax.Nsid", "com.atproto.syntax.Nsid"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "datetimeParam", "java.time.Instant", "java.time.Instant"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "languageParam", "java.util.Locale", "java.util.Locale"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "uriParam", "java.net.URI", "java.net.URI"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "uriRefParam", "java.net.URI", "java.net.URI"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "uriTemplateParam", "java.lang.String", "java.lang.String"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "emailParam", "java.lang.String", "java.lang.String"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "hostnameParam", "java.lang.String", "java.lang.String"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "ipv4Param", "java.net.InetAddress", "java.net.InetAddress"));
        argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                stringFormatParams),
                "ipv6Param", "java.net.InetAddress", "java.net.InetAddress"));

        return argList.stream();
    }
}