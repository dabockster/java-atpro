package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import com.atproto.api.AtpResponse;
import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.lexicon.models.*;
import com.atproto.common.Cid;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
public class ClientGeneratorMockingTest {

        @Mock
        private XrpcClient mockXrpcClient; // Mock the XrpcClient

        private ClientGenerator generator;

        @BeforeEach
        public void setUp() {
                generator = new ClientGenerator(); // Initialize in setup for each test
        }

        // Assume TestUtils helpers like createSimpleQueryLexicon,
        // createProcedureLexicon etc. are available
        // (copied/adapted from ClientGeneratorTest or a shared TestUtils class)

        @Test
        public void testGenerateClientForSimpleQuery_Success_WithHeaders() throws Exception { // Renamed test
                LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.simpleQuery"
                String generatedClassName = "com.example.SimpleQueryClient";
                String generatedMethodName = "simpleQuery"; // Method name derived from lexicon ID

                // --- Arrange ---
                // Define custom headers
                Map<String, String> customHeaders = Map.of("X-Custom-Header", "TestValue123");
                Optional<Map<String, String>> optionalHeaders = Optional.of(customHeaders);

                // Mock response
                AtpResponse<Void> mockSuccessResponse = new AtpResponse<>(200, Optional.empty(),
                                Map.of("Content-Type", List.of("application/json")));
                CompletableFuture<AtpResponse<Void>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendQuery method to expect the specific headers
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // No params
                                eq(optionalHeaders) // Expect the specific headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Generate and compile
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method, now expecting a headers argument
                // The generated method signature needs to accept Optional<Map<String, String>>
                java.lang.reflect.Method simpleQueryMethod = generatedClientClass.getMethod(generatedMethodName, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleQueryMethod
                                .invoke(clientInstance, optionalHeaders); // Pass headers

                // --- Assert ---
                AtpResponse<?> actualResponse = resultFuture.get();

                // Verify sendQuery was called exactly once with the correct arguments, including headers
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(),
                                eq(optionalHeaders) // Verify the specific headers were passed
                );

                // Check the response
                assertNotNull(actualResponse);
                assertEquals(200, actualResponse.getStatusCode());
                assertFalse(actualResponse.getResponse().isPresent());
                assertNotNull(actualResponse.getHeaders());
                assertTrue(actualResponse.getHeaders().containsKey("Content-Type"));
        }

        @Test
        public void testGenerateClientForSimpleQuery_Error() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.simpleQuery"
                String generatedClassName = "com.example.SimpleQueryClient";
                String generatedMethodName = "simpleQuery"; // Method name derived from lexicon ID

                // --- Arrange ---
                // Create a simulated XRPCException
                XrpcException simXrpcException = new XrpcException(400, "Bad Request", Optional.empty());

                // Stub the sendQuery method to throw the XrpcException wrapped in a
                // CompletableFuture
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(CompletableFuture.failedFuture(simXrpcException));

                // --- Act ---
                // Generate the client code
                String generatedCode = generator.generateClient(lexiconDoc);

                // Compile the generated code
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method using reflection
                // Assuming the method without headers still exists or we test the one with Optional.empty() headers
                 java.lang.reflect.Method simpleQueryMethod = generatedClientClass.getMethod(generatedMethodName, Optional.class);
                 CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleQueryMethod
                                 .invoke(clientInstance, Optional.empty()); // Pass empty headers for error test


                // --- Assert ---
                // Wait for the future to complete and check for an exception
                Exception exception = assertThrows(ExecutionException.class, () -> resultFuture.get());

                assertTrue(exception.getCause() instanceof XrpcException);
                XrpcException xrpcException = (XrpcException) exception.getCause();

                // Verify sendQuery was called exactly once with the correct arguments
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                );

                // Check the exception details
                assertNotNull(xrpcException);
                assertEquals(400, xrpcException.getStatusCode());
                assertEquals("Bad Request", xrpcException.getMessage());
                assertFalse(xrpcException.getResponse().isPresent());
        }

        @Test
        public void testGenerateClientForSimpleProcedure_Success() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createSimpleProcedureLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.simpleProcedure"
                String generatedClassName = "com.example.SimpleProcedureClient";
                String generatedMethodName = "simpleProcedure"; // Method name derived from lexicon ID

                // --- Arrange ---
                // Create a mock input object for the procedure
                LexiconDoc.Input inputDoc = lexiconDoc.getDefinitions().get("main").getProcedure().getInput()
                                .orElseThrow();
                LexObject inputLexObject = (LexObject) inputDoc.getType();
                Map<String, LexPrimitive> properties = inputLexObject.getProperties();
                Map<String, Object> inputValues = new HashMap<>();
                for (Map.Entry<String, LexPrimitive> entry : properties.entrySet()) {
                        // Populate with dummy values for now
                        inputValues.put(entry.getKey(), "testValue");
                }

                String inputClassName = "com.example.SimpleProcedureProcedureInput";

                // Create a mock AtpResponse with expected data
                AtpResponse<Void> mockSuccessResponse = new AtpResponse<>(200, Optional.empty(),
                                Map.of("Content-Type", List.of("application/json"))); // Optional<Void> for no output
                                                                                      // body
                CompletableFuture<AtpResponse<Void>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendProcedure method to return the mock response wrapped in a
                // CompletableFuture
                when(mockXrpcClient.sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Object>any(), // Required input object
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Generate the client code
                String generatedCode = generator.generateClient(lexiconDoc);

                // Compile the generated code
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Create an instance of the procedure input class using reflection
                // Find the inner class for input
                Class<?> inputClass = null;
                for (Class<?> innerClass : generatedClientClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals("ProcedureInput")) {
                                inputClass = innerClass;
                                break;
                        }
                }
                assertNotNull(inputClass, "Could not find inner ProcedureInput class");

                Object inputObject = inputClass.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                        java.lang.reflect.Field field = inputClass.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(inputObject, entry.getValue());
                }

                // Invoke the generated method using reflection
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method simpleProcedureMethod = generatedClientClass.getMethod(generatedMethodName,
                                inputClass, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleProcedureMethod
                                .invoke(clientInstance, inputObject, Optional.empty()); // Pass empty headers


                // --- Assert ---
                // Wait for the future to complete and retrieve the result
                AtpResponse<?> actualResponse = resultFuture.get(); // This should be the mock response

                // Verify sendProcedure was called exactly once with the correct arguments
                verify(mockXrpcClient, times(1)).sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Object>any(), // Required input object
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                );

                // Check the response
                assertNotNull(actualResponse);
                assertEquals(200, actualResponse.getStatusCode());
                assertFalse(actualResponse.getResponse().isPresent()); // No response body expected
                assertNotNull(actualResponse.getHeaders());
                assertTrue(actualResponse.getHeaders().containsKey("Content-Type"));
        }

        @Test
        public void testGenerateClientForSimpleProcedure_Error() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createSimpleProcedureLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.simpleProcedure"
                String generatedClassName = "com.example.SimpleProcedureClient";
                String generatedMethodName = "simpleProcedure"; // Method name derived from lexicon ID

                // --- Arrange ---
                // Create a mock input object for the procedure
                LexiconDoc.Input inputDoc = lexiconDoc.getDefinitions().get("main").getProcedure().getInput()
                                .orElseThrow();
                LexObject inputLexObject = (LexObject) inputDoc.getType();
                Map<String, LexPrimitive> properties = inputLexObject.getProperties();
                Map<String, Object> inputValues = new HashMap<>();
                for (Map.Entry<String, LexPrimitive> entry : properties.entrySet()) {
                        // Populate with dummy values for now
                        inputValues.put(entry.getKey(), "testValue");
                }

                String inputClassName = "com.example.SimpleProcedureProcedureInput";

                // Create a simulated XRPCException
                XrpcException simXrpcException = new XrpcException(400, "Bad Request", Optional.empty());

                // Stub the sendProcedure method to throw the XrpcException wrapped in a
                // CompletableFuture
                when(mockXrpcClient.sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Object>any(), // Required input object
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(CompletableFuture.failedFuture(simXrpcException));

                // --- Act ---
                // Generate the client code
                String generatedCode = generator.generateClient(lexiconDoc);

                // Compile the generated code
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Create an instance of the procedure input class using reflection
                 // Find the inner class for input
                Class<?> inputClass = null;
                for (Class<?> innerClass : generatedClientClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals("ProcedureInput")) {
                                inputClass = innerClass;
                                break;
                        }
                }
                assertNotNull(inputClass, "Could not find inner ProcedureInput class");

                Object inputObject = inputClass.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                        java.lang.reflect.Field field = inputClass.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(inputObject, entry.getValue());
                }

                // Invoke the generated method using reflection
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method simpleProcedureMethod = generatedClientClass.getMethod(generatedMethodName,
                                inputClass, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleProcedureMethod
                                .invoke(clientInstance, inputObject, Optional.empty()); // Pass empty headers


                // --- Assert ---
                // Wait for the future to complete and check for an exception
                Exception exception = assertThrows(ExecutionException.class, () -> resultFuture.get());

                assertTrue(exception.getCause() instanceof XrpcException);
                XrpcException xrpcException = (XrpcException) exception.getCause();

                // Verify sendProcedure was called exactly once with the correct arguments
                verify(mockXrpcClient, times(1)).sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Object>any(), // Required input object
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                );

                // Check the exception details
                assertNotNull(xrpcException);
                assertEquals(400, xrpcException.getStatusCode());
                assertEquals("Bad Request", xrpcException.getMessage());
                assertFalse(xrpcException.getResponse().isPresent());
        }

        @Test
        public void testGenerateClientForQueryWithParams_Success() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createQueryWithParamsLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.paramsQuery"
                String generatedClassName = "com.example.ParamsQueryClient";
                String generatedMethodName = "paramsQuery";

                // --- Arrange ---
                String testStringParam = "testValue";
                Integer testIntParam = 123;

                // Expected parameters map for verification
                Map<String, Object> expectedParams = new HashMap<>();
                expectedParams.put("p_string", testStringParam);
                expectedParams.put("p_int", testIntParam);

                // Mock response
                AtpResponse<Void> mockSuccessResponse = new AtpResponse<>(200, Optional.empty(),
                                Map.of("Content-Type", List.of("application/json")));
                CompletableFuture<AtpResponse<Void>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendQuery method
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                eq(Optional.of(expectedParams)), // Expect the parameters map
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Generate and compile
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method with parameters
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method paramsQueryMethod = generatedClientClass.getMethod(generatedMethodName,
                                String.class, Integer.class, Optional.class); // Method signature with params + headers
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) paramsQueryMethod
                                .invoke(clientInstance, testStringParam, testIntParam, Optional.empty()); // Pass empty headers


                // --- Assert ---
                AtpResponse<?> actualResponse = resultFuture.get();

                // Verify sendQuery was called with correct parameters
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                eq(Optional.of(expectedParams)), // Verify the map was passed
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the response
                assertNotNull(actualResponse);
                assertEquals(200, actualResponse.getStatusCode());
                assertFalse(actualResponse.getResponse().isPresent());
                assertNotNull(actualResponse.getHeaders());
                assertTrue(actualResponse.getHeaders().containsKey("Content-Type"));
        }

        @Test
        public void testGenerateClientForQueryWithParams_Error() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createQueryWithParamsLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.paramsQuery"
                String generatedClassName = "com.example.ParamsQueryClient";
                String generatedMethodName = "paramsQuery";

                // --- Arrange ---
                String testStringParam = "testValue";
                Integer testIntParam = 123;

                // Expected parameters map for verification
                Map<String, Object> expectedParams = new HashMap<>();
                expectedParams.put("p_string", testStringParam);
                expectedParams.put("p_int", testIntParam);

                // Simulated error
                XrpcException simXrpcException = new XrpcException(500, "Internal Server Error", Optional.empty());

                // Stub the sendQuery method to throw error
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                eq(Optional.of(expectedParams)), // Expect the parameters map
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(CompletableFuture.failedFuture(simXrpcException));

                // --- Act ---
                // Generate and compile
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method with parameters
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method paramsQueryMethod = generatedClientClass.getMethod(generatedMethodName,
                                String.class, Integer.class, Optional.class); // Method signature with params + headers
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) paramsQueryMethod
                                .invoke(clientInstance, testStringParam, testIntParam, Optional.empty()); // Pass empty headers


                // --- Assert ---
                // Check for exception
                Exception exception = assertThrows(ExecutionException.class, () -> resultFuture.get());

                assertTrue(exception.getCause() instanceof XrpcException);
                XrpcException xrpcException = (XrpcException) exception.getCause();

                // Verify sendQuery was called with correct parameters
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                eq(Optional.of(expectedParams)), // Verify the map was passed
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the exception details
                assertNotNull(xrpcException);
                assertEquals(500, xrpcException.getStatusCode());
                assertEquals("Internal Server Error", xrpcException.getMessage());
                assertFalse(xrpcException.getResponse().isPresent());
        }

        @Test
        public void testGenerateClientForQueryWithOutput_Success() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createQueryWithOutputLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.outputQuery"
                String generatedClassName = "com.example.OutputQueryClient";
                String generatedMethodName = "outputQuery";
                String outputClassName = generatedClassName + "$QueryOutput"; // Inner class for output

                // --- Arrange ---
                // Create a mock output object instance
                Class<?> outputClass = null; // We'll get this after compilation
                Object mockOutputObject = null; // We'll create this after compilation

                // Mock response containing the output object
                // We need to compile first to get the output class type
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Find the inner output class
                for (Class<?> innerClass : generatedClientClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals("QueryOutput")) {
                                outputClass = innerClass;
                                break;
                        }
                }
                assertNotNull(outputClass, "Could not find inner QueryOutput class");

                // Create and populate the mock output object
                mockOutputObject = outputClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field messageField = outputClass.getDeclaredField("message");
                messageField.setAccessible(true);
                messageField.set(mockOutputObject, "Success!");
                java.lang.reflect.Field countField = outputClass.getDeclaredField("count");
                countField.setAccessible(true);
                countField.set(mockOutputObject, 42);


                AtpResponse<?> mockSuccessResponse = new AtpResponse<>(200, Optional.of(mockOutputObject),
                                Map.of("Content-Type", List.of("application/json")));
                CompletableFuture<AtpResponse<?>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendQuery method
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // No params for this query
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method outputQueryMethod = generatedClientClass.getMethod(generatedMethodName, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) outputQueryMethod
                                .invoke(clientInstance, Optional.empty()); // Pass empty headers


                // --- Assert ---
                AtpResponse<?> actualResponse = resultFuture.get();

                // Verify sendQuery was called
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(),
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the response and its body
                assertNotNull(actualResponse);
                assertEquals(200, actualResponse.getStatusCode());
                assertTrue(actualResponse.getResponse().isPresent());
                assertNotNull(actualResponse.getHeaders());
                assertTrue(actualResponse.getHeaders().containsKey("Content-Type"));

                // Verify the output object content
                Object actualOutputObject = actualResponse.getResponse().get();
                assertNotNull(actualOutputObject);
                assertEquals(outputClass, actualOutputObject.getClass()); // Check type

                assertEquals("Success!", messageField.get(actualOutputObject));
                assertEquals(42, countField.get(actualOutputObject));
        }

        @Test
        public void testGenerateClientForQueryWithOutput_Error() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createQueryWithOutputLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.outputQuery"
                String generatedClassName = "com.example.OutputQueryClient";
                String generatedMethodName = "outputQuery";

                // --- Arrange ---
                // Simulated error
                XrpcException simXrpcException = new XrpcException(404, "Not Found", Optional.empty());

                // Stub the sendQuery method to throw error
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // No params
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(CompletableFuture.failedFuture(simXrpcException));

                // --- Act ---
                // Generate and compile
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method outputQueryMethod = generatedClientClass.getMethod(generatedMethodName, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) outputQueryMethod
                                .invoke(clientInstance, Optional.empty()); // Pass empty headers


                // --- Assert ---
                // Check for exception
                Exception exception = assertThrows(ExecutionException.class, () -> resultFuture.get());

                assertTrue(exception.getCause() instanceof XrpcException);
                XrpcException xrpcException = (XrpcException) exception.getCause();

                // Verify sendQuery was called
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(),
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the exception details
                assertNotNull(xrpcException);
                assertEquals(404, xrpcException.getStatusCode());
                assertEquals("Not Found", xrpcException.getMessage());
                assertFalse(xrpcException.getResponse().isPresent());
        }

        @Test
        public void testGenerateClientForUploadBlob_Success() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createUploadBlobLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.atproto.repo.uploadBlob"
                String generatedClassName = "com.atproto.repo.UploadBlobClient";
                String generatedMethodName = "uploadBlob";
                String outputClassName = generatedClassName + "$ProcedureOutput"; // Inner class for output

                // --- Arrange ---
                byte[] mockInputData = "mock blob data".getBytes();
                Cid mockCid = Cid.of("bafkreihdwdcefgh4dqkjv67uzcmw7ojee6xedzdetojuzjevtenxquvyku"); // Example CID

                // Compile first to get the output class type
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Find the inner output class
                Class<?> outputClass = null;
                for (Class<?> innerClass : generatedClientClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals("ProcedureOutput")) {
                                outputClass = innerClass;
                                break;
                        }
                }
                assertNotNull(outputClass, "Could not find inner ProcedureOutput class");

                // Create and populate the mock output object
                Object mockOutputObject = outputClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field blobField = outputClass.getDeclaredField("blob");
                blobField.setAccessible(true);
                blobField.set(mockOutputObject, mockCid); // Set the mock Cid

                // Mock response containing the output object
                AtpResponse<?> mockSuccessResponse = new AtpResponse<>(200, Optional.of(mockOutputObject),
                                Map.of("Content-Type", List.of("application/json")));
                CompletableFuture<AtpResponse<?>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendProcedure method for byte array input
                when(mockXrpcClient.sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // No params
                                ArgumentMatchers.<byte[]>any(), // Expect byte array input
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method with byte array
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method uploadBlobMethod = generatedClientClass.getMethod(generatedMethodName, byte[].class, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) uploadBlobMethod
                                .invoke(clientInstance, mockInputData, Optional.empty()); // Pass empty headers


                // --- Assert ---
                AtpResponse<?> actualResponse = resultFuture.get();

                // Verify sendProcedure was called with byte array
                verify(mockXrpcClient, times(1)).sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(),
                                eq(mockInputData), // Verify the byte array was passed
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the response and its body
                assertNotNull(actualResponse);
                assertEquals(200, actualResponse.getStatusCode());
                assertTrue(actualResponse.getResponse().isPresent());
                assertNotNull(actualResponse.getHeaders());
                assertTrue(actualResponse.getHeaders().containsKey("Content-Type"));

                // Verify the output object content
                Object actualOutputObject = actualResponse.getResponse().get();
                assertNotNull(actualOutputObject);
                assertEquals(outputClass, actualOutputObject.getClass()); // Check type

                assertEquals(mockCid, blobField.get(actualOutputObject)); // Check the CID
        }

        @Test
        public void testGenerateClientForUploadBlob_Error() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createUploadBlobLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.atproto.repo.uploadBlob"
                String generatedClassName = "com.atproto.repo.UploadBlobClient";
                String generatedMethodName = "uploadBlob";

                // --- Arrange ---
                byte[] mockInputData = "mock blob data".getBytes();

                // Simulated error
                XrpcException simXrpcException = new XrpcException(413, "Payload Too Large", Optional.empty());

                // Stub the sendProcedure method to throw error
                when(mockXrpcClient.sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // No params
                                ArgumentMatchers.<byte[]>any(), // Expect byte array input
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(CompletableFuture.failedFuture(simXrpcException));

                // --- Act ---
                // Generate and compile
                String generatedCode = generator.generateClient(lexiconDoc);
                Class<?> generatedClientClass = TestUtils.InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create instance and inject mock
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method with byte array
                // Assuming headers are optional and added as the last parameter
                java.lang.reflect.Method uploadBlobMethod = generatedClientClass.getMethod(generatedMethodName, byte[].class, Optional.class);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) uploadBlobMethod
                                .invoke(clientInstance, mockInputData, Optional.empty()); // Pass empty headers


                // --- Assert ---
                // Check for exception
                Exception exception = assertThrows(ExecutionException.class, () -> resultFuture.get());

                assertTrue(exception.getCause() instanceof XrpcException);
                XrpcException xrpcException = (XrpcException) exception.getCause();

                // Verify sendProcedure was called
                verify(mockXrpcClient, times(1)).sendProcedure(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(),
                                eq(mockInputData), // Verify the byte array was passed
                                ArgumentMatchers.<Optional<Map<String, String>>>any()
                );

                // Check the exception details
                assertNotNull(xrpcException);
                assertEquals(413, xrpcException.getStatusCode());
                assertEquals("Payload Too Large", xrpcException.getMessage());
                assertFalse(xrpcException.getResponse().isPresent());
        }

} // End of class ClientGeneratorMockingTest