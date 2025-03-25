//src/test/java/com/atproto/codegen/TestUtils.java

package com.atproto.codegen;

import com.atproto.lexicon.models.*;

import javax.tools.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

public class TestUtils {

        public static LexiconDoc createSimpleQueryLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.simpleQuery", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createSimpleQueryLexiconWithDescription() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.of("This is a test query."),
                                Optional.empty(),
                                Optional.of(output), new ArrayList<>()); // Added description
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.simpleQueryWithDescription", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createQueryWithParamsLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();
                params.put("p_string",
                                new LexString(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                params.put("p_int",
                                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"), Optional.empty(), params,
                                new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(), Optional.empty(),
                                Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.paramsQuery", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createProcedureLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                Map<String, LexPrimitive> properties = new HashMap<>();
                properties.put("p_string", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));

                LexXrpcBody input = new LexXrpcBody("application/json", Optional.of(new LexObject(
                                Optional.empty(), Optional.empty(), properties,
                                new ArrayList<>())), Optional.empty());

                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());

                LexXrpcProcedure procedure = new LexXrpcProcedure(Optional.of(input), Optional.empty(),
                                Optional.of(output),
                                new ArrayList<>());
                defs.add(new LexDefinition("main", "procedure", procedure));

                return new LexiconDoc(1, "com.example.procedure", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createSubscriptionLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                Map<String, LexPrimitive> properties = new HashMap<>(); // Properties, for message
                properties.put("p_string", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty())); // Add string property

                LexXrpcSubscription subscription = new LexXrpcSubscription(Optional.empty(), Optional.empty()); // Declare
                                                                                                                // subscription

                defs.add(new LexDefinition("main", "subscription", subscription)); // Add to Definitions.

                return new LexiconDoc(1, "com.example.subscription", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createMultiMethodLexicon() { // Multiple Method Lexicon.
                List<LexDefinition> defs = new ArrayList<>();

                // Query
                LexXrpcBody outputQuery = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty()); // Create Output

                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.of(outputQuery), new ArrayList<>()); // Create Query
                defs.add(new LexDefinition("queryMethod", "query", query)); // Add Definition

                // Procedure
                LexXrpcBody inputProcedure = new LexXrpcBody("application/json", Optional.empty(), Optional.empty()); // Create
                                                                                                                      // input
                LexXrpcBody outputProcedure = new LexXrpcBody("application/json", Optional.empty(), Optional.empty()); // Create
                                                                                                                       // output

                LexXrpcProcedure procedure = new LexXrpcProcedure(Optional.of(inputProcedure), Optional.empty(),
                                Optional.of(outputProcedure), new ArrayList<>()); // Create Procedure

                defs.add(new LexDefinition("procedureMethod", "procedure", procedure)); // Add Definition

                return new LexiconDoc(1, "com.example.multiMethod", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity()))); // Convert Def List to Map
        }

        private static LexiconDoc createDuplicateMethodLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody outputQuery = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.of(outputQuery), new ArrayList<>());
                defs.add(new LexDefinition("queryMethod", "query", query));
                defs.add(new LexDefinition("queryMethod", "query", query)); // Duplicate

                return new LexiconDoc(1, "com.example.duplicateMethod", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createNestedObjectLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                // Define the nested object type
                Map<String, LexPrimitive> nestedProperties = new HashMap<>();
                nestedProperties.put("innerString", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                LexObject nestedObject = new LexObject(Optional.empty(), Optional.empty(), nestedProperties,
                                new ArrayList<>());

                // Main query with an object containing the nested object
                Map<String, LexType> properties = new HashMap<>();
                properties.put("outerObject", nestedObject);

                LexXrpcBody output = new LexXrpcBody("application/json",
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(), properties,
                                                new ArrayList<>())),
                                Optional.empty());

                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.of(output),
                                new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.nestedObject", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithParams(String id, Map<String, ? extends LexType> params) {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"), Optional.empty(), params,
                                new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(), Optional.empty(),
                                Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, id, Optional.of(0), Optional.empty(), defs.stream().collect(
                                java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithRefUnionParams() {
                List<LexDefinition> defs = new ArrayList<>();

                // Define the referenced types
                Map<String, LexPrimitive> recordDef1Props = new HashMap<>();
                recordDef1Props.put("name1", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                LexRecord recordDef1 = new LexRecord(Optional.of("object"), Optional.empty(), Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(), recordDef1Props,
                                                new ArrayList<>())),
                                Optional.empty());

                Map<String, LexPrimitive> recordDef2Props = new HashMap<>();
                recordDef2Props.put("name2",
                                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                LexRecord recordDef2 = new LexRecord(Optional.of("object"), Optional.empty(), Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(), recordDef2Props,
                                                new ArrayList<>())),
                                Optional.empty());

                defs.add(new LexDefinition("recordDef1", "record", recordDef1));
                defs.add(new LexDefinition("recordDef2", "record", recordDef2));

                // Main query with a ref-union parameter
                Map<String, LexType> params = new HashMap<>();
                List<String> refs = List.of("#recordDef1", "#recordDef2");
                params.put("refUnionParam", new LexRefUnion(refs, Optional.empty()));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"), Optional.empty(), params,
                                new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(), Optional.empty(),
                                Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.refUnionParams", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithMultipleDefs() {
                List<LexDefinition> defs = new ArrayList<>();

                LexXrpcBody output1 = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query1 = new LexXrpcQuery(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.of(output1), new ArrayList<>());
                defs.add(new LexDefinition("query1", "query", query1));

                Map<String, LexPrimitive> params2 = new HashMap<>();
                params2.put("param1",
                                new LexString(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                LexXrpcParameters xrpcParams2 = new LexObject(Optional.of("params"), Optional.empty(), params2,
                                new ArrayList<>());

                LexXrpcBody output2 = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query2 = new LexXrpcQuery(Optional.of(xrpcParams2), Optional.empty(), Optional.empty(),
                                Optional.of(output2), new ArrayList<>());
                defs.add(new LexDefinition("query2", "query", query2));

                Map<String, LexPrimitive> recordProperties = new HashMap<>();
                recordProperties.put("name", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                LexRecord recordDef = new LexRecord(Optional.of("object"), Optional.empty(), Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(), recordProperties,
                                                new ArrayList<>())),
                                Optional.empty());
                defs.add(new LexDefinition("record1", "record", recordDef));

                return new LexiconDoc(1, "com.example.multipleDefs", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithoutDefs() {
                // Create a LexiconDoc without 'defs'. This is invalid.
                return new LexiconDoc(1, "com.example.invalid", Optional.of(0), Optional.empty(), Map.of());

        }

        public static LexiconDoc createLexiconWithInvalidType() {
                // Create a LexiconDoc with an invalid parameter type within a query.
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();
                // Add an invalid type.
                params.put("invalidParam", new LexString(Optional.of("invalidtype"), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"), Optional.empty(), params,
                                new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());

                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(), Optional.empty(),
                                Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query)); //

                return new LexiconDoc(1, "com.example.invalidType", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static InputStream stringToInputStream(String str) {
                return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        }

        public static LexiconDoc createLexiconWithStringConstraints() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();

                // String with maxLength
                params.put("maxLengthString", new LexString(Optional.empty(), Optional.empty(), Optional.of(10),
                                Optional.empty(), Optional.empty()));

                // String with minLength
                params.put("minLengthString", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.of(5), Optional.empty()));

                // String with const
                params.put("constString", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(List.of("constantValue"))));

                // String with pattern
                params.put("patternString", new LexString(Optional.empty(), Optional.of("[a-zA-Z]+"), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                // String with enum

                List<String> enumValues = Arrays.asList("value1", "value2", "value3");
                params.put("enumString", new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(enumValues)));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"), Optional.empty(), params,
                                new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(), Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(), Optional.empty(),
                                Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.stringConstraints", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static Stream<Arguments> provideLexiconsForStringConstraints() {
                return Stream.of(
                                Arguments.of(createLexiconWithStringConstraints(), "maxLengthString", "String", 10,
                                                null, null,
                                                null), // maxLength
                                Arguments.of(createLexiconWithStringConstraints(), "minLengthString", "String", null, 5,
                                                null,
                                                null), // minLength
                                Arguments.of(createLexiconWithStringConstraints(), "constString", "String", null, null,
                                                "constantValue",
                                                null), // const value
                                Arguments.of(createLexiconWithStringConstraints(), "patternString", "String", null,
                                                null, null,
                                                "[a-zA-Z]+") // Regex pattern
                // Arguments.of(createLexiconWithStringConstraints(), "enumString", "String",
                // null, null, null,enumValues)
                );
        }

        public static LexiconDoc createLexiconQueryNoOutput() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(), Optional.empty(),
                                Optional.empty(), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.NoOutputQuery", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));

        }

        public static LexiconDoc createLexiconProcedureNoOutput() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcProcedure proc = new LexXrpcProcedure(Optional.empty(), Optional.empty(), Optional.empty(),
                                new ArrayList<>());
                defs.add(new LexDefinition("main", "procedure", proc));
                return new LexiconDoc(1, "com.example.NoOutputProcedure", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(LexDefinition::getId,
                                                java.util.function.Function.identity())));

        }

        public static class InMemoryCompiler {
                private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

                public static Class<?> compile(String className, String sourceCode)
                                throws URISyntaxException, ClassNotFoundException {
                        // Use try-with-resources to ensure closure
                        try (JavaFileManager fileManager = new ClassFileManager(
                                        compiler.getStandardFileManager(null, null, null))) {

                                List<JavaFileObject> compilationUnits = new ArrayList<>();
                                compilationUnits.add(new SourceFileObject(className, sourceCode));

                                // Create a compilation task
                                JavaCompiler.CompilationTask task = compiler.getTask(
                                                null, // No writer, write to memory.
                                                fileManager,
                                                null, // No diagnostics listener
                                                null, // No options
                                                null, // No classes to be processed (for annotation processing)
                                                compilationUnits);

                                // Perform the compilation
                                boolean success = task.call();

                                if (!success) {
                                        // For proper error reporting, we need to collect the diagnostics.
                                        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                                        JavaFileManager fileManager2 = new ClassFileManager(
                                                        compiler.getStandardFileManager(diagnostics, null, null));
                                        compiler.getTask(
                                                        null, // No writer, write to memory.
                                                        fileManager2,
                                                        diagnostics,
                                                        null, // No options
                                                        null, // No classes to be processed
                                                        compilationUnits).call(); // Don't check success; we want to see
                                                                                  // the diagnostics in either case.

                                        StringBuilder errorMsg = new StringBuilder();
                                        errorMsg.append("Compilation failed:\n");
                                        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics
                                                        .getDiagnostics()) {
                                                errorMsg.append(diagnostic.toString()).append("\n");

                                        }
                                        throw new RuntimeException(errorMsg.toString());
                                }

                                // Load the compiled class
                                return fileManager.getClassLoader(null).loadClass(className);
                        } catch (IOException e) {
                                throw new RuntimeException("IOException during in-memory compilation", e);
                        }
                }

                private static class SourceFileObject extends SimpleJavaFileObject {
                        private final String sourceCode;

                        SourceFileObject(String name, String sourceCode) throws URISyntaxException {
                                super(new URI("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                                                Kind.SOURCE);
                                this.sourceCode = sourceCode;
                        }

                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                                return CharBuffer.wrap(sourceCode);
                        }
                }

                private static class ClassFileObject extends SimpleJavaFileObject {
                        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                        ClassFileObject(String name, Kind kind) throws URISyntaxException {
                                super(new URI("byte:///" + name.replace('.', '/') + kind.extension), kind);
                        }

                        byte[] getBytes() {
                                return outputStream.toByteArray();
                        }

                        @Override
                        public OutputStream openOutputStream() {
                                return outputStream;
                        }
                }

                private static class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
                        private final Map<String, ClassFileObject> compiledClasses = new HashMap<>();

                        ClassFileManager(JavaFileManager fileManager) {
                                super(fileManager);
                        }

                        @Override
                        public ClassLoader getClassLoader(Location location) {
                                return new ClassLoader() {
                                        @Override
                                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                                ClassFileObject classFile = compiledClasses.get(name);
                                                if (classFile == null) {
                                                        throw new ClassNotFoundException(name);
                                                }
                                                byte[] bytes = classFile.getBytes();
                                                return defineClass(name, bytes, 0, bytes.length);
                                        }
                                };
                        }

                        @Override
                        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                        JavaFileObject.Kind kind,
                                        FileObject sibling) throws IOException {
                                try {
                                        ClassFileObject fileObject = new ClassFileObject(className, kind);
                                        compiledClasses.put(className, fileObject); // Store the compiled class

                                        return fileObject;
                                } catch (URISyntaxException ex) {
                                        throw new RuntimeException(ex);
                                }
                        }
                }
        }
}
