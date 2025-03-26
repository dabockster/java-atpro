// src/main/java/com/atproto/codegen/InMemoryCompiler.java

package com.atproto.codegen;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Collectors;


public class InMemoryCompiler {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public static Class<?> compile(String className, String sourceCode)
            throws URISyntaxException, ClassNotFoundException {

        if (compiler == null) {
            throw new IllegalStateException("System Java Compiler not found. Ensure you are running with a JDK, not just a JRE.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        // Use try-with-resources for the file manager
        try (JavaFileManager fileManager = new ClassFileManager(
                compiler.getStandardFileManager(diagnostics, null, null))) {

            List<JavaFileObject> compilationUnits = List.of(
                    new SourceFileObject(className, sourceCode));

            // Create a compilation task
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, // No writer, use System.err for diagnostics if needed
                    fileManager,
                    diagnostics,
                    null, // No options
                    null, // No classes to be processed (for annotation processing)
                    compilationUnits);

            // Perform the compilation
            boolean success = task.call();

            if (!success) {
                StringBuilder errorMsg = new StringBuilder("Compilation failed for " + className + ":\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorMsg.append(String.format("Code: %s%n", diagnostic.getCode()));
                    errorMsg.append(String.format("Kind: %s%n", diagnostic.getKind()));
                    errorMsg.append(String.format("Position: %d%n", diagnostic.getPosition()));
                    errorMsg.append(String.format("Start Position: %d%n", diagnostic.getStartPosition()));
                    errorMsg.append(String.format("End Position: %d%n", diagnostic.getEndPosition()));
                    errorMsg.append(String.format("Source: %s%n", diagnostic.getSource()));
                    errorMsg.append(String.format("Message: %s%n", diagnostic.getMessage(null))); // Use null locale for default message
                    errorMsg.append("\n---\n");
                }
                 // Include source code in the error for easier debugging
                errorMsg.append("\n--- Source Code ---\n");
                errorMsg.append(sourceCode);
                errorMsg.append("\n--- End Source Code ---\n");
                throw new RuntimeException(errorMsg.toString());
            }

            // Load the compiled class using the custom file manager's class loader
            return fileManager.getClassLoader(null).loadClass(className);

        } catch (IOException e) {
             throw new RuntimeException("IOException during in-memory compilation for " + className, e);
        }
    }

    private static class SourceFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        SourceFileObject(String name, String sourceCode) throws URISyntaxException {
            super(new URI("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            // Wrap the source code in a CharBuffer
             return CharBuffer.wrap(this.sourceCode);
        }
    }

    private static class ClassFileObject extends SimpleJavaFileObject {
        // Use ByteArrayOutputStream to hold the compiled bytecode in memory
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ClassFileObject(String name, Kind kind) throws URISyntaxException {
             // The URI scheme "byte:///" indicates in-memory storage
            super(new URI("byte:///" + name.replace('.', '/') + kind.extension), kind);
        }

        // Returns the compiled bytecode as a byte array
        byte[] getBytes() {
            return outputStream.toByteArray();
        }

        // Override openOutputStream to return the in-memory stream
        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }
    }

    // Custom JavaFileManager to manage compiled classes in memory
    private static class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
         // Holds the compiled ClassFileObjects, keyed by class name
        private final Map<String, ClassFileObject> compiledClasses = new HashMap<>();

        ClassFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
             // Return a custom ClassLoader that loads classes from compiledClasses map
            return new ClassLoader(super.getClassLoader(location)) { // Delegate to parent classloader first
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    ClassFileObject classFile = compiledClasses.get(name);
                    if (classFile != null) {
                        byte[] bytes = classFile.getBytes();
                         // Define the class from the bytecode byte array
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    // If not found in memory, delegate to parent
                    return super.findClass(name);
                }
            };
        }

        // Called by the compiler to get the output file for a compiled class
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                FileObject sibling) throws IOException {
            try {
                // Create a new ClassFileObject to hold the bytecode
                ClassFileObject fileObject = new ClassFileObject(className, kind);
                compiledClasses.put(className, fileObject); // Store it in the map
                return fileObject;
            } catch (URISyntaxException ex) {
                // Wrap URI syntax errors in a RuntimeException
                 throw new RuntimeException("Error creating URI for generated class file: " + className, ex);
            }
        }
    }
}