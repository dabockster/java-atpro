// src/test/java/com/atproto/codegen/TestUtils.java

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
import java.util.HashMap; // Ensure HashMap is imported
import com.atproto.common.Cid; // Import Cid if not already present

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.assertj.core.api.Assertions;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.owasp.dependencycheck.utils.DependencyCheckException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@PrepareForTest({TestUtils.class})
public class TestUtils {
    @Mock
    private JavaCompiler compilerMock;

    @Mock
    private JavaFileManager fileManagerMock;

    @BeforeEach
    public void setUp() {
        PowerMockito.mockStatic(ToolProvider.class);
        PowerMockito.when(ToolProvider.getSystemJavaCompiler()).thenReturn(compilerMock);
    }

    @ParameterizedTest
    @MethodSource("provideLexiconParams")
    public void testCreateLexiconWithParams(String id, Map<String, ? extends LexType> params) {
        LexiconDoc doc = createLexiconWithParams(id, params);
        Assertions.assertThat(doc).isNotNull();
        Assertions.assertThat(doc.getId()).isEqualTo(id);
        Assertions.assertThat(doc.getDefinitions()).isNotEmpty();
    }

    private static Stream<Arguments> provideLexiconParams() {
        return Stream.of(
            Arguments.of("com.example.test", new HashMap<>()),
            Arguments.of("com.example.test2", Map.of("param1", new LexString()))
        );
    }

    @Test
    public void testInMemoryCompiler() throws Exception {
        String className = "TestClass";
        String sourceCode = "public class TestClass { public void test() {} }";

        Class<?> compiledClass = InMemoryCompiler.compile(className, sourceCode);
        Assertions.assertThat(compiledClass).isNotNull();
        Assertions.assertThat(compiledClass.getName()).isEqualTo(className);
    }

    public static LexiconDoc createLexiconWithParams(String id,
                        Map<String, ? extends LexType> params) {
        List<LexDefinition> defs = new ArrayList<>();
        LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
        LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
        LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
        defs.add(new LexDefinition("main", "query", query));
        return new LexiconDoc(1, id, Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
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
                                JavaCompiler.CompilationTask task = compiler.getTask(null, // No
                                                                                           // writer,
                                                                                           // write
                                                                                           // to
                                                                                           // memory.
                                                fileManager, null, // No diagnostics listener
                                                null, // No options
                                                null, // No classes to be processed (for annotation
                                                      // processing)
                                                compilationUnits);

                                // Perform the compilation
                                boolean success = task.call();

                                if (!success) {
                                        // For proper error reporting, we need to collect the
                                        // diagnostics.
                                        DiagnosticCollector<JavaFileObject> diagnostics =
                                                        new DiagnosticCollector<>();
                                        JavaFileManager fileManager2 = new ClassFileManager(
                                                        compiler.getStandardFileManager(diagnostics,
                                                                        null, null));
                                        compiler.getTask(null, // No writer, write to memory.
                                                        fileManager2, diagnostics, null, // No
                                                                                         // options
                                                        null, // No classes to be processed
                                                        compilationUnits).call(); // Don't check
                                                                                  // success; we
                                                                                  // want to see
                                                                                  // the diagnostics
                                                                                  // in either case.

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
                                throw new RuntimeException(
                                                "IOException during in-memory compilation", e);
                        }
                }

                private static class SourceFileObject extends SimpleJavaFileObject {
                        private final String sourceCode;

                        SourceFileObject(String name, String sourceCode) throws URISyntaxException {
                                super(new URI("string:///" + name.replace('.', '/')
                                                + Kind.SOURCE.extension), Kind.SOURCE);
                                this.sourceCode = sourceCode;
                        }

                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                                return CharBuffer.wrap(sourceCode);
                        }
                }

                private static class ClassFileObject extends SimpleJavaFileObject {
                        private final ByteArrayOutputStream outputStream =
                                        new ByteArrayOutputStream();

                        ClassFileObject(String name, Kind kind) throws URISyntaxException {
                                super(new URI("byte:///" + name.replace('.', '/') + kind.extension),
                                                kind);
                        }

                        byte[] getBytes() {
                                return outputStream.toByteArray();
                        }

                        @Override
                        public OutputStream openOutputStream() {
                                return outputStream;
                        }
                }

                private static class ClassFileManager
                                extends ForwardingJavaFileManager<JavaFileManager> {
                        private final Map<String, ClassFileObject> compiledClasses =
                                        new HashMap<>();

                        ClassFileManager(JavaFileManager fileManager) {
                                super(fileManager);
                        }

                        @Override
                        public ClassLoader getClassLoader(Location location) {
                                return new ClassLoader() {
                                        @Override
                                        protected Class<?> findClass(String name)
                                                        throws ClassNotFoundException {
                                                ClassFileObject classFile =
                                                                compiledClasses.get(name);
                                                if (classFile == null) {
                                                        throw new ClassNotFoundException(name);
                                                }
                                                byte[] bytes = classFile.getBytes();
                                                return defineClass(name, bytes, 0, bytes.length);
                                        }
                                };
                        }

                        @Override
                        public JavaFileObject getJavaFileForOutput(Location location,
                                        String className, JavaFileObject.Kind kind,
                                        FileObject sibling) throws IOException {
                                try {
                                        ClassFileObject fileObject =
                                                        new ClassFileObject(className, kind);
                                        compiledClasses.put(className, fileObject); // Store the
                                                                                    // compiled
                                                                                    // class

                                        return fileObject;
                                } catch (URISyntaxException ex) {
                                        throw new RuntimeException(ex);
                                }
                        }
                }
        }

        public static LexiconDoc createSimpleQueryLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.simpleQuery", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createSimpleQueryLexiconWithDescription() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(),
                                Optional.of("This is a test query."), Optional.empty(),
                                Optional.of(output), new ArrayList<>()); // Added
                                                                         // description
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.simpleQueryWithDescription", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createQueryWithParamsLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();
                params.put("p_string", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                params.put("p_int", new LexInteger(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.paramsQuery", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createProcedureLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                Map<String, LexPrimitive> properties = new HashMap<>();
                properties.put("p_string", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));

                LexXrpcBody input = new LexXrpcBody(
                                "application/json", Optional.of(new LexObject(Optional.empty(),
                                                Optional.empty(), properties, new ArrayList<>())),
                                Optional.empty());

                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());

                LexXrpcProcedure procedure = new LexXrpcProcedure(Optional.of(input),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "procedure", procedure));

                return new LexiconDoc(1, "com.example.procedure", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createSubscriptionLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                Map<String, LexPrimitive> properties = new HashMap<>(); // Properties, for message
                properties.put("p_string", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty())); // Add
                                                                                        // string
                                                                                        // property

                LexXrpcSubscription subscription =
                                new LexXrpcSubscription(Optional.empty(), Optional.empty()); // Declare
                // subscription

                defs.add(new LexDefinition("main", "subscription", subscription)); // Add to
                                                                                   // Definitions.

                return new LexiconDoc(1, "com.example.subscription", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createMultiMethodLexicon() { // Multiple Method Lexicon.
                List<LexDefinition> defs = new ArrayList<>();

                // Query
                LexXrpcBody outputQuery = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty()); // Create
                                                   // Output

                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(outputQuery), new ArrayList<>()); // Create
                                                                                                // Query
                defs.add(new LexDefinition("queryMethod", "query", query)); // Add Definition

                // Procedure
                LexXrpcBody inputProcedure = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty()); // Create
                // input
                LexXrpcBody outputProcedure = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty()); // Create
                // output

                LexXrpcProcedure procedure = new LexXrpcProcedure(Optional.of(inputProcedure),
                                Optional.empty(), Optional.of(outputProcedure), new ArrayList<>()); // Create
                                                                                                    // Procedure

                defs.add(new LexDefinition("procedureMethod", "procedure", procedure)); // Add
                                                                                        // Definition

                return new LexiconDoc(1, "com.example.multiMethod", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity()))); // Convert
                                                                                           // Def
                                                                                           // List
                                                                                           // to Map
        }

        private static LexiconDoc createDuplicateMethodLexicon() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody outputQuery = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(outputQuery), new ArrayList<>());
                defs.add(new LexDefinition("queryMethod", "query", query));
                defs.add(new LexDefinition("queryMethod", "query", query)); // Duplicate

                return new LexiconDoc(1, "com.example.duplicateMethod", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createNestedObjectLexicon() {
                List<LexDefinition> defs = new ArrayList<>();

                // Define the nested object type
                Map<String, LexPrimitive> nestedProperties = new HashMap<>();
                nestedProperties.put("innerString",
                                new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty(), Optional.empty()));
                LexObject nestedObject = new LexObject(Optional.empty(), Optional.empty(),
                                nestedProperties, new ArrayList<>());

                // Main query with an object containing the nested object
                Map<String, LexType> properties = new HashMap<>();
                properties.put("outerObject", nestedObject);

                LexXrpcBody output = new LexXrpcBody(
                                "application/json", Optional.of(new LexObject(Optional.empty(),
                                                Optional.empty(), properties, new ArrayList<>())),
                                Optional.empty());

                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.nestedObject", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithParams(String id,
                        Map<String, ? extends LexType> params) {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, id, Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithRefUnionParams() {
                List<LexDefinition> defs = new ArrayList<>();

                // Define the referenced types
                Map<String, LexPrimitive> recordDef1Props = new HashMap<>();
                recordDef1Props.put("name1", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                LexRecord recordDef1 = new LexRecord(Optional.of("object"), Optional.empty(),
                                Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(),
                                                recordDef1Props, new ArrayList<>())),
                                Optional.empty());

                Map<String, LexPrimitive> recordDef2Props = new HashMap<>();
                recordDef2Props.put("name2", new LexInteger(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                LexRecord recordDef2 = new LexRecord(Optional.of("object"), Optional.empty(),
                                Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(),
                                                recordDef2Props, new ArrayList<>())),
                                Optional.empty());

                defs.add(new LexDefinition("recordDef1", "record", recordDef1));
                defs.add(new LexDefinition("recordDef2", "record", recordDef2));

                // Main query with a ref-union parameter
                Map<String, LexType> params = new HashMap<>();
                List<String> refs = List.of("#recordDef1", "#recordDef2");
                params.put("refUnionParam", new LexRefUnion(refs, Optional.empty()));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.refUnionParams", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithMultipleDefs() {
                List<LexDefinition> defs = new ArrayList<>();

                LexXrpcBody output1 = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query1 = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output1), new ArrayList<>());
                defs.add(new LexDefinition("query1", "query", query1));

                Map<String, LexPrimitive> params2 = new HashMap<>();
                params2.put("param1", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                LexXrpcParameters xrpcParams2 = new LexObject(Optional.of("params"),
                                Optional.empty(), params2, new ArrayList<>());

                LexXrpcBody output2 = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query2 = new LexXrpcQuery(Optional.of(xrpcParams2), Optional.empty(),
                                Optional.empty(), Optional.of(output2), new ArrayList<>());
                defs.add(new LexDefinition("query2", "query", query2));

                Map<String, LexPrimitive> recordProperties = new HashMap<>();
                recordProperties.put("name", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                LexRecord recordDef = new LexRecord(Optional.of("object"), Optional.empty(),
                                Optional.empty(),
                                Optional.of(new LexObject(Optional.empty(), Optional.empty(),
                                                recordProperties, new ArrayList<>())),
                                Optional.empty());
                defs.add(new LexDefinition("record1", "record", recordDef));

                return new LexiconDoc(1, "com.example.multipleDefs", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithStringConstraints() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();

                // String with maxLength
                params.put("maxLengthString", new LexString(Optional.empty(), Optional.empty(),
                                Optional.of(10), Optional.empty(), Optional.empty()));

                // String with minLength
                params.put("minLengthString", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(5), Optional.empty()));

                // String with const
                params.put("constString",
                                new LexString(Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty(),
                                                Optional.of(List.of("constantValue"))));

                // String with pattern
                params.put("patternString",
                                new LexString(Optional.empty(), Optional.of("[a-zA-Z]+"),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                // String with enum

                List<String> enumValues = Arrays.asList("value1", "value2", "value3");
                params.put("enumString", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.of(enumValues)));

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.stringConstraints", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static Stream<Arguments> provideLexiconsForStringConstraints() {
                return Stream.of(
                                Arguments.of(createLexiconWithStringConstraints(),
                                                "maxLengthString", "String", 10, null, null, null), // maxLength
                                Arguments.of(createLexiconWithStringConstraints(),
                                                "minLengthString", "String", null, 5, null, null), // minLength
                                Arguments.of(createLexiconWithStringConstraints(), "constString",
                                                "String", null, null, "constantValue", null), // const
                                                                                              // value
                                Arguments.of(createLexiconWithStringConstraints(), "patternString",
                                                "String", null, null, null, "[a-zA-Z]+") // Regex
                                                                                         // pattern
                // Arguments.of(createLexiconWithStringConstraints(), "enumString", "String",
                // null, null, null,enumValues)
                );
        }

        public static LexiconDoc createLexiconQueryNoOutput() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.NoOutputQuery", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));

        }

        public static LexiconDoc createLexiconProcedureNoOutput() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcProcedure proc = new LexXrpcProcedure(Optional.empty(), Optional.empty(),
                                Optional.empty(), new ArrayList<>());
                defs.add(new LexDefinition("main", "procedure", proc));
                return new LexiconDoc(1, "com.example.NoOutputProcedure", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
}

public static LexiconDoc createQueryWithOutputLexicon() {
        List<LexDefinition> defs = new ArrayList<>();

        // Define the output object schema
        Map<String, LexPrimitive> outputProperties = new HashMap<>();
        outputProperties.put("message", new LexString(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()));
        outputProperties.put("count", new LexInteger(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()));
        LexObject outputObject = new LexObject(Optional.empty(), Optional.empty(),
                        outputProperties, new ArrayList<>());

        LexXrpcBody output = new LexXrpcBody("application/json", Optional.of(outputObject),
                        Optional.empty());

        LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.of(output), new ArrayList<>());
        defs.add(new LexDefinition("main", "query", query));

        return new LexiconDoc(1, "com.example.outputQuery", Optional.of(0),
                        Optional.empty(),
                        defs.stream().collect(java.util.stream.Collectors.toMap(
                                        LexDefinition::getId,
                                        java.util.function.Function.identity())));
}

public static LexiconDoc createUploadBlobLexicon() {
        List<LexDefinition> defs = new ArrayList<>();

        // Define the output object schema containing a CidLink
        Map<String, LexType> outputProperties = new HashMap<>();
        outputProperties.put("blob", new LexCidLink(Optional.empty(), Optional.empty()));
        LexObject outputObject = new LexObject(Optional.empty(), Optional.empty(),
                        outputProperties, List.of("blob")); // blob is required

        LexXrpcBody output = new LexXrpcBody("application/json", Optional.of(outputObject),
                        Optional.empty());

        // Define the input body (accepts any content type, typically image/* or similar)
        LexXrpcBody input = new LexXrpcBody("*/*", Optional.empty(), Optional.empty());


        LexXrpcProcedure procedure = new LexXrpcProcedure(Optional.of(input),
                        Optional.empty(), Optional.of(output), new ArrayList<>());
        defs.add(new LexDefinition("main", "procedure", procedure));

        return new LexiconDoc(1, "com.atproto.repo.uploadBlob", Optional.of(0),
                        Optional.empty(),
                        defs.stream().collect(java.util.stream.Collectors.toMap(
                                        LexDefinition::getId,
                                        java.util.function.Function.identity())));
}

// ---------- INVALID LEXICON CREATION METHODS ----------

public static LexiconDoc createLexiconWithoutDefs() {
                // Create a LexiconDoc without 'defs'. This is invalid.
                return new LexiconDoc(1, "com.example.invalid.nodefs", Optional.of(0),
                                Optional.empty(), Map.of());
        }

        public static LexiconDoc createLexiconWithInvalidIdFormat() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                // Invalid ID format (missing parts)
                return new LexiconDoc(1, "invalid-id", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithConflictingDefinitions() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                // Add the *same* definition twice (same ID).
                defs.add(new LexDefinition("main", "query", query));
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.conflictingdefs", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId, // This
                                                                      // will
                                                                      // NOW
                                                                      // fail!!!!
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithInvalidArrayDefinition_Nested() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexType> params = new HashMap<>();
                // Nested array of arrays (invalid). Lexicon only supports top-level arrays.
                params.put("nestedArray",
                                new LexArray(new LexArray(
                                                new LexInteger(Optional.empty(), Optional.empty(),
                                                                Optional.empty(), Optional.empty()),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()), Optional.empty(),
                                                Optional.empty(), Optional.empty()));
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.invalidarray.nested", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithInvalidArrayDefinition_MissingItems() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexType> params = new HashMap<>();

                // Create an invalid LexArray - items are required (should not allow an empty
                // Optional)
                LexArray invalidArray = mock(LexArray.class);
                when(invalidArray.getItems())
                                .thenThrow(new NullPointerException("Items cannot be null")); // Simulate
                                                                                              // missing
                                                                                              // field.
                when(invalidArray.getType()).thenReturn("array");

                params.put("invalidArray", invalidArray);

                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));
                return new LexiconDoc(1, "com.example.invalidarray.missingitems", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithInvalidRefTarget() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexType> params = new HashMap<>();
                // Ref to a non-existent definition
                params.put("invalidRef", new LexRef("#missing", Optional.empty()));
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.invalidref", Optional.of(0), Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithInvalidRefUnionTarget() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexType> params = new HashMap<>();
                // RefUnion with a reference to a non-existent type.
                params.put("invalidRefUnion",
                                new LexRefUnion(List.of("#missingType"), Optional.empty()));
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.invalidrefunion", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        public static LexiconDoc createLexiconWithInvalidStringFormat() {
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();
                // String with an invalid format
                params.put("invalidFormatString",
                                new LexString(Optional.of("invalid-format"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query));

                return new LexiconDoc(1, "com.example.invalidstringformat", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        // Utility to create a LexiconDoc from a raw (potentially invalid) Map.
        public static LexiconDoc createLexiconFromRawMap(Map<String, Object> rawLexicon) {
                return LexiconDoc.fromJson(rawLexicon);
        }

        public static InputStream stringToInputStream(String str) {
                return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
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
                                JavaCompiler.CompilationTask task = compiler.getTask(null, // No
                                                                                           // writer,
                                                                                           // write
                                                                                           // to
                                                                                           // memory.
                                                fileManager, null, // No diagnostics listener
                                                null, // No options
                                                null, // No classes to be processed (for annotation
                                                      // processing)
                                                compilationUnits);

                                // Perform the compilation
                                boolean success = task.call();

                                if (!success) {
                                        // For proper error reporting, we need to collect the
                                        // diagnostics.
                                        DiagnosticCollector<JavaFileObject> diagnostics =
                                                        new DiagnosticCollector<>();
                                        JavaFileManager fileManager2 = new ClassFileManager(
                                                        compiler.getStandardFileManager(diagnostics,
                                                                        null, null));
                                        compiler.getTask(null, // No writer, write to memory.
                                                        fileManager2, diagnostics, null, // No
                                                                                         // options
                                                        null, // No classes to be processed
                                                        compilationUnits).call(); // Don't check
                                                                                  // success; we
                                                                                  // want to see
                                                                                  // the diagnostics
                                                                                  // in either case.

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
                                throw new RuntimeException(
                                                "IOException during in-memory compilation", e);
                        }
                }

                private static class SourceFileObject extends SimpleJavaFileObject {
                        private final String sourceCode;

                        SourceFileObject(String name, String sourceCode) throws URISyntaxException {
                                super(new URI("string:///" + name.replace('.', '/')
                                                + Kind.SOURCE.extension), Kind.SOURCE);
                                this.sourceCode = sourceCode;
                        }

                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                                return CharBuffer.wrap(sourceCode);
                        }
                }

                private static class ClassFileObject extends SimpleJavaFileObject {
                        private final ByteArrayOutputStream outputStream =
                                        new ByteArrayOutputStream();

                        ClassFileObject(String name, Kind kind) throws URISyntaxException {
                                super(new URI("byte:///" + name.replace('.', '/') + kind.extension),
                                                kind);
                        }

                        byte[] getBytes() {
                                return outputStream.toByteArray();
                        }

                        @Override
                        public OutputStream openOutputStream() {
                                return outputStream;
                        }
                }

                private static class ClassFileManager
                                extends ForwardingJavaFileManager<JavaFileManager> {
                        private final Map<String, ClassFileObject> compiledClasses =
                                        new HashMap<>();

                        ClassFileManager(JavaFileManager fileManager) {
                                super(fileManager);
                        }

                        @Override
                        public ClassLoader getClassLoader(Location location) {
                                return new ClassLoader() {
                                        @Override
                                        protected Class<?> findClass(String name)
                                                        throws ClassNotFoundException {
                                                ClassFileObject classFile =
                                                                compiledClasses.get(name);
                                                if (classFile == null) {
                                                        throw new ClassNotFoundException(name);
                                                }
                                                byte[] bytes = classFile.getBytes();
                                                return defineClass(name, bytes, 0, bytes.length);
                                        }
                                };
                        }

                        @Override
                        public JavaFileObject getJavaFileForOutput(Location location,
                                        String className, JavaFileObject.Kind kind,
                                        FileObject sibling) throws IOException {
                                try {
                                        ClassFileObject fileObject =
                                                        new ClassFileObject(className, kind);
                                        compiledClasses.put(className, fileObject); // Store the
                                                                                    // compiled
                                                                                    // class

                                        return fileObject;
                                } catch (URISyntaxException ex) {
                                        throw new RuntimeException(ex);
                                }
                        }
                }
        }

        public static LexiconDoc createLexiconWithInvalidType() {
                // Create a LexiconDoc with an invalid parameter type within a query.
                List<LexDefinition> defs = new ArrayList<>();
                Map<String, LexPrimitive> params = new HashMap<>();
                // Add an invalid type.
                params.put("invalidParam",
                                new LexString(Optional.of("invalidtype"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty())); // invalid type
                LexXrpcParameters xrpcParams = new LexObject(Optional.of("params"),
                                Optional.empty(), params, new ArrayList<>());
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());

                LexXrpcQuery query = new LexXrpcQuery(Optional.of(xrpcParams), Optional.empty(),
                                Optional.empty(), Optional.of(output), new ArrayList<>());
                defs.add(new LexDefinition("main", "query", query)); //

                return new LexiconDoc(1, "com.example.invalidType", Optional.of(0),
                                Optional.empty(),
                                defs.stream().collect(java.util.stream.Collectors.toMap(
                                                LexDefinition::getId,
                                                java.util.function.Function.identity())));
        }

        private static Stream<Arguments> provideLexiconsForInvalidLexVersions() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(),
                                Optional.of("This is a test query."), Optional.empty(),
                                Optional.of(output), new ArrayList<>()); // Added
                                                                         // description
                defs.add(new LexDefinition("main", "query", query));

                return Stream.of(
                                // lex value of 0 is invalid
                                Arguments.of(new LexiconDoc(0, "com.example.invalidVersion",
                                                Optional.of(0), Optional.empty(),
                                                defs.stream().collect(java.util.stream.Collectors
                                                                .toMap(LexDefinition::getId,
                                                                                java.util.function.Function
                                                                                                .identity()))),
                                                IllegalArgumentException.class) // Invalid Version

                );
        }

        // Added Valid Lex Version test

        private static Stream<Arguments> provideLexiconsForValidLexVersions() {
                List<LexDefinition> defs = new ArrayList<>();
                LexXrpcBody output = new LexXrpcBody("application/json", Optional.empty(),
                                Optional.empty());
                LexXrpcQuery query = new LexXrpcQuery(Optional.empty(),
                                Optional.of("This is a test query."), Optional.empty(),
                                Optional.of(output), new ArrayList<>()); // Added
                                                                         // description
                defs.add(new LexDefinition("main", "query", query));

                return Stream.of(
                                // lex value of 1
                                Arguments.of(new LexiconDoc(1, "com.example.validversion",
                                                Optional.of(0), Optional.empty(),
                                                defs.stream().collect(java.util.stream.Collectors
                                                                .toMap(LexDefinition::getId,
                                                                                java.util.function.Function
                                                                                                .identity()))))

                );
        }

        private static Stream<Arguments> provideLexiconsForAllParameterTypes() {
                List<Arguments> argList = new ArrayList<>();

                // Integer types
                Map<String, LexPrimitive> intParams = new HashMap<>();
                intParams.put("intParam", new LexInteger(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.intParams",
                                intParams), "intParam", "Integer"));

                // Number types (float/double) part of LexNumber
                Map<String, LexPrimitive> numberParams = new HashMap<>();
                numberParams.put("floatParam", new LexNumber(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(TestUtils.createLexiconWithParams(
                                "com.example.floatParams", numberParams), "floatParam", "Float")); // Double,
                                                                                                   // double

                // String types
                Map<String, LexPrimitive> stringParams = new HashMap<>();
                stringParams.put("stringParam", new LexString(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(TestUtils
                                .createLexiconWithParams("com.example.stringParams", stringParams),
                                "stringParam", "String"));

                // Boolean types
                Map<String, LexPrimitive> boolParams = new HashMap<>();
                boolParams.put("boolParam", new LexBoolean(Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(TestUtils.createLexiconWithParams("com.example.boolParams",
                                boolParams), "boolParam", "Boolean"));

                // Bytes type
                Map<String, LexPrimitive> bytesParams = new HashMap<>();
                bytesParams.put("bytesParam", new LexBytes(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(TestUtils.createLexiconWithParams(
                                "com.example.bytesParams", bytesParams), "bytesParam", "byte[]"));

                // CidLink
                Map<String, LexPrimitive> cidLinkParams = new HashMap<>();
                cidLinkParams.put("cidLinkParam",
                                new LexCidLink(Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.cidLinkParams",
                                                cidLinkParams),
                                "cidLinkParam", "com.atproto.common.Cid"));

                // Array of primitives
                Map<String, LexType> arrayParams = new HashMap<>();
                arrayParams.put("intArrayParam", new LexArray(
                                new LexInteger(Optional.empty(), Optional.empty(), Optional.empty(),
                                                Optional.empty()),
                                Optional.empty(), Optional.empty(), Optional.empty()));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.arrayParams",
                                                arrayParams),
                                "intArrayParam", "java.util.List<Integer>"));

                // Unknown
                Map<String, LexPrimitive> unknownParams = new HashMap<>();
                unknownParams.put("unknownParam", new LexUnknown(Optional.empty()));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.unknownParams",
                                                unknownParams),
                                "unknownParam", "java.util.Map<String, Object>"));

                // String Formats.
                Map<String, LexPrimitive> stringFormatParams = new HashMap<>();
                stringFormatParams.put("atUriParam",
                                new LexString(Optional.of("at-uri"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("cidParam",
                                new LexString(Optional.of("cid"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("didParam",
                                new LexString(Optional.of("did"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("handleParam",
                                new LexString(Optional.of("handle"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("nsidParam",
                                new LexString(Optional.of("nsid"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("datetimeParam",
                                new LexString(Optional.of("datetime"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("languageParam",
                                new LexString(Optional.of("language"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("uriParam",
                                new LexString(Optional.of("uri"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("uriRefParam",
                                new LexString(Optional.of("uri-reference"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("uriTemplateParam",
                                new LexString(Optional.of("uri-template"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("emailParam",
                                new LexString(Optional.of("email"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("hostnameParam",
                                new LexString(Optional.of("hostname"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("ipv4Param",
                                new LexString(Optional.of("ipv4"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                stringFormatParams.put("ipv6Param",
                                new LexString(Optional.of("ipv6"), Optional.empty(),
                                                Optional.empty(), Optional.empty(),
                                                Optional.empty()));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "atUriParam", "com.atproto.syntax.AtUri"));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "cidParam", "com.atproto.common.Cid")); // Assuming you have a
                                                                        // Cid
                                                                        // class.

                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "didParam", "com.atproto.syntax.Did")); // Assuming you have a
                                                                        // Did
                                                                        // class

                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "handleParam", "com.atproto.syntax.Handle")); // Assuming you
                                                                              // have a
                                                                              // Handle class

                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "nsidParam", "com.atproto.syntax.Nsid")); // Assuming you have
                                                                          // an
                                                                          // NSID
                                                                          // class.

                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "datetimeParam", "java.time.Instant"));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "languageParam", "java.util.Locale"));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "uriParam", "java.net.URI"));
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "uriRefParam", "java.net.URI")); // Assuming URI for
                                                                 // uri-reference
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "uriTemplateParam", "java.lang.String")); // Assuming String
                                                                          // for
                                                                          // uri-template (no
                                                                          // built-in
                                                                          // type)
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "emailParam", "java.lang.String")); // Assuming String for
                                                                    // email
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "hostnameParam", "java.lang.String")); // Assuming String for
                                                                       // hostname
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "ipv4Param", "java.net.InetAddress")); // Assuming InetAddress
                                                                       // for
                                                                       // IPv4
                argList.add(Arguments.of(
                                TestUtils.createLexiconWithParams("com.example.stringFormatParams",
                                                stringFormatParams),
                                "ipv6Param", "java.net.InetAddress")); // Assuming InetAddress
                                                                       // for
                                                                       // IPv6

                return argList.stream();
        }

        private static Stream<Arguments> provideLexiconsForRefUnionParams() {
                return Stream.of(Arguments.of(TestUtils.createLexiconWithRefUnionParams(),
                                "refUnionParams", "java.lang.Object") // Object
                                                                      // for
                                                                      // now,
                                                                      // may
                                                                      // be
                                                                      // refined
                );
        }
}
