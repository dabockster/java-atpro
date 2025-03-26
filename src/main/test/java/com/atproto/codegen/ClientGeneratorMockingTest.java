// src/main/test/java/com/atproto/codegen/ClientGeneratorMockingTest.java

package com.atproto.codegen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atproto.api.AtpResponse;
import com.atproto.api.xrpc.XrpcClient;
import com.atproto.api.xrpc.XrpcException;
import com.atproto.lexicon.models.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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

@ExtendWith(MockitoExtension.class) // Use MockitoExtension for JUnit 5 integration
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
        public void testGenerateClientForSimpleQuery_Success() throws Exception {
                LexiconDoc lexiconDoc = TestUtils.createSimpleQueryLexicon();
                String lexiconId = lexiconDoc.getId(); // "com.example.simpleQuery"
                String generatedClassName = "com.example.SimpleQueryClient";
                String generatedMethodName = "simpleQuery"; // Method name derived from lexicon ID

                // --- Arrange ---
                // Create a mock AtpResponse with expected data (assuming Void or a simple type)
                // Let's assume void output for simplicity
                AtpResponse<Void> mockSuccessResponse = new AtpResponse<>(200, Optional.empty(),
                                Map.of("Content-Type", List.of("application/json"))); // Optional<Void> for no output
                                                                                      // body
                CompletableFuture<AtpResponse<Void>> futureResponse = CompletableFuture
                                .completedFuture(mockSuccessResponse);

                // Stub the sendQuery method to return the mock response wrapped in a
                // CompletableFuture
                when(mockXrpcClient.sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
                                ArgumentMatchers.<Optional<Map<String, String>>>any() // Optional headers
                )).thenReturn(futureResponse);

                // --- Act ---
                // Generate the client code
                String generatedCode = generator.generateClient(lexiconDoc);

                // Compile the generated code
                Class<?> generatedClientClass = InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method using reflection
                java.lang.reflect.Method simpleQueryMethod = generatedClientClass.getMethod(generatedMethodName);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleQueryMethod
                                .invoke(clientInstance);

                // --- Assert ---
                // Wait for the future to complete and retrieve the result
                AtpResponse<?> actualResponse = resultFuture.get(); // This should be the mock response

                // Verify sendQuery was called exactly once with the correct arguments
                verify(mockXrpcClient, times(1)).sendQuery(
                                eq(lexiconId),
                                ArgumentMatchers.<Optional<Object>>any(), // Optional params (none here)
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
                Class<?> generatedClientClass = InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Invoke the generated method using reflection
                java.lang.reflect.Method simpleQueryMethod = generatedClientClass.getMethod(generatedMethodName);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleQueryMethod
                                .invoke(clientInstance);

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
                Class<?> generatedClientClass = InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Create an instance of the procedure input class using reflection
                Class<?> inputClass = Class.forName(inputClassName);
                Object inputObject = inputClass.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                        java.lang.reflect.Field field = inputClass.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(inputObject, entry.getValue());
                }

                // Invoke the generated method using reflection
                java.lang.reflect.Method simpleProcedureMethod = generatedClientClass.getMethod(generatedMethodName,
                                inputClass);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleProcedureMethod
                                .invoke(clientInstance, inputObject);

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
                Class<?> generatedClientClass = InMemoryCompiler.compile(generatedClassName, generatedCode);

                // Create an instance of the generated client
                Object clientInstance = generatedClientClass.getDeclaredConstructor().newInstance();

                // Inject the mockXrpcClient
                java.lang.reflect.Field xrpcClientField = generatedClientClass.getDeclaredField("xrpcClient");
                xrpcClientField.setAccessible(true);
                xrpcClientField.set(clientInstance, mockXrpcClient);

                // Create an instance of the procedure input class using reflection
                Class<?> inputClass = Class.forName(inputClassName);
                Object inputObject = inputClass.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : inputValues.entrySet()) {
                        java.lang.reflect.Field field = inputClass.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(inputObject, entry.getValue());
                }

                // Invoke the generated method using reflection
                java.lang.reflect.Method simpleProcedureMethod = generatedClientClass.getMethod(generatedMethodName,
                                inputClass);
                CompletableFuture<AtpResponse<?>> resultFuture = (CompletableFuture<AtpResponse<?>>) simpleProcedureMethod
                                .invoke(clientInstance, inputObject);

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
}