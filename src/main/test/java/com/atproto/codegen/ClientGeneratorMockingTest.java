// src/test/java/com/atproto/codegen/ClientGeneratorMockingTest.java

package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atproto.api.AtpResponse;
import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.lexicon.models.LexDefinition;
import com.atproto.lexicon.models.LexXrpcBody;
import com.atproto.lexicon.models.LexXrpcQuery;
import com.atproto.lexicon.models.LexiconDoc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Use MockitoExtension for JUnit 5 integration
public class ClientGeneratorMockingTest {

    @Mock
    private XrpcClient mockXrpcClient; // Mock the XrpcClient

    private ClientGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new ClientGenerator(); // Initialize in setup for each test
    }

    private LexiconDoc createSimpleQueryLexicon() {
        // Helper method from ClientGeneratorTest - make sure it or a similar utility
        // is accessible. For now, I'm assuming it's in a common test utility class.
        // We can refactor this later if needed.

        List<LexDefinition> defs = new ArrayList<>();
        LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
        LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(output), new ArrayList<>());
        defs.add(new LexDefinition("main", "query", query));

        return new LexiconDoc(
                1,
                "com.example.simpleQuery",
                Optional.of(0),
                Optional.empty(),
                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                        java.util.function.Function.identity())));
    }

    private LexiconDoc createSimpleProcedureLexicon() {
        List<LexDefinition> defs = new ArrayList<>();

        // Define request body (if any)
        Map<String, com.atproto.lexicon.models.LexPrimitive> properties = new java.util.HashMap<>();
        properties.put("message", new com.atproto.lexicon.models.LexString(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));

        com.atproto.lexicon.models.LexXrpcBody input = new com.atproto.lexicon.models.LexXrpcBody("application/json",
                Optional.of(new com.atproto.lexicon.models.LexObject(Optional.empty(), Optional.empty(), properties,
                        new ArrayList<>())),
                Optional.empty());

        // Define response body (if any).
        com.atproto.lexicon.models.LexXrpcBody output = new com.atproto.lexicon.models.LexXrpcBody("application/json",
                Optional.empty(),
                Optional.empty());

        com.atproto.lexicon.models.LexXrpcProcedure procedure = new com.atproto.lexicon.models.LexXrpcProcedure(
                Optional.of(input),
                Optional.empty(),
                Optional.of(output),
                new ArrayList<>());

        defs.add(new LexDefinition("main", "procedure", procedure));

        return new LexiconDoc(
                1,
                "com.example.simpleProcedure",
                Optional.of(0),
                Optional.empty(),
                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                        java.util.function.Function.identity())));

    }

    @Test
    public void testGenerateClientForSimpleQuery_Success() throws Exception {
        LexiconDoc lexiconDoc = createSimpleQueryLexicon();

        // Create a mock AtpResponse with expected data
        AtpResponse<String> mockResponse = new AtpResponse<>(200, "{\"message\": \"success\"}", Map.of());// Use a map

        // Stub the sendQuery method to return the mock response
        when(mockXrpcClient.sendQuery(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Generate the client code
        String generatedCode = generator.generateClient(lexiconDoc);

        // --- Compilation and Reflection (Important) ---
        // 1. Compile the generated code. This is the CRUCIAL step
        Class<?> generatedClientClass = InMemoryCompiler.compile("com.example.SimpleQueryClient", generatedCode);

        // 2. Create an instance of the generated client.
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // 3. Inject the mockXrpcClient into the generated client instance.
        // This is where we replace the *real* XrpcClient with our *mock*.
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true); // Allow access to the private field
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // 4. Invoke the generated method (using reflection).
        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery"); // Get the method.
        Object result = method.invoke(clientInstance); // Invoke the method.

        // --- Assertions ---
        // Verify that sendQuery was called *exactly once* with the expected arguments.
        verify(mockXrpcClient, times(1)).sendQuery(eq("com.example.simpleQuery"), eq(Optional.empty()),
                eq(Optional.empty()));

        // Assert on the result from calling client's method (using the mocked repsonse)
        assertInstanceOf(AtpResponse.class, result); // The result is of type AtpResponse of Map.
        AtpResponse<?> atpResponse = (AtpResponse<?>) result; // Cast to AtpResponse

        assertEquals(200, atpResponse.getStatusCode());
        assertEquals("{\"message\": \"success\"}", atpResponse.getResponse()); // Check for mocked return.

    }

    @Test
    public void testGenerateClientForSimpleQuery_Error() throws Exception { // Test XRPC error cases
        LexiconDoc lexiconDoc = createSimpleQueryLexicon();

        // Stub the sendQuery method to throw an XRPCException.
        when(mockXrpcClient.sendQuery(anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new XRPCException(400, "Bad Request", Optional.empty()))); // Simulate
                                                                                                                      // error.

        // Generate the client code
        String generatedCode = generator.generateClient(lexiconDoc);

        // --- Compilation and Reflection (Important) ---

        Class<?> generatedClientClass = InMemoryCompiler.compile("com.example.SimpleQueryClient", generatedCode);

        // 2. Create an instance of the generated client.
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // 3. Inject the mockXrpcClient into the generated client instance.
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true);
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // 4. Invoke the generated method (using reflection).
        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleQuery");

        // --- Assertions ---

        // Verify that sendQuery was called *exactly once* with the expected arguments.
        // Use assertThrows to verify that the expected exception is thrown.
        XRPCException thrown = assertThrows(
                XRPCException.class,
                () -> method.invoke(clientInstance),
                "Expected sendQuery to throw, but it didn't");

        assertEquals(400, thrown.getStatusCode());
        assertEquals("Bad Request", thrown.getMessage());
    }

    @Test
    public void testGenerateClientForSimpleProcedure_Success() throws Exception {
        LexiconDoc lexiconDoc = createSimpleProcedureLexicon();

        // Create a mock AtpResponse with expected data
        AtpResponse<String> mockResponse = new AtpResponse<>(200, "{\"message\": \"success\"}", Map.of());// Use a map

        // Stub the sendQuery method to return the mock response
        when(mockXrpcClient.sendProcedure(anyString(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Generate the client code
        String generatedCode = generator.generateClient(lexiconDoc);

        // --- Compilation and Reflection (Important) ---
        // 1. Compile the generated code. This is the CRUCIAL step
        Class<?> generatedClientClass = InMemoryCompiler.compile("com.example.SimpleProcedureClient", generatedCode);

        // 2. Create an instance of the generated client.
        Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

        // 3. Inject the mockXrpcClient into the generated client instance.
        // This is where we replace the *real* XrpcClient with our *mock*.
        java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
        xrpcClientField.setAccessible(true); // Allow access to the private field
        xrpcClientField.set(clientInstance, mockXrpcClient);

        // 4. Invoke the generated method (using reflection).
        // Since the generated code requires an input, prepare an input class and
        // instantiate an object to provide.

        Class<?> inputClass = Class.forName("com.example.SimpleProcedureProcedureInput");
        Object input = inputClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method method = generatedClientClass.getMethod("simpleProcedure", inputClass); // Get the
                                                                                                         // method
                                                                                                         // signature
        Object result = method.invoke(clientInstance, input); // Invoke the method.

        // --- Assertions ---
        // Verify that sendQuery was called *exactly once* with the expected arguments.
        verify(mockXrpcClient, times(1)).sendProcedure(eq("com.example.simpleProcedure"), eq(Optional.empty()), any(),
                eq(Optional.empty()));

        // Assert on the result from calling client's method (using the mocked repsonse)
        assertInstanceOf(AtpResponse.class, result); // The result is of type AtpResponse of Map.
        AtpResponse<?> atpResponse = (AtpResponse<?>) result; // Cast to AtpResponse

        assertEquals(200, atpResponse.getStatusCode());
        assertEquals("{\"message\": \"success\"}", atpResponse.getResponse()); // Check for mocked return.

    }
}
