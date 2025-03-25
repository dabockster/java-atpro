package com.atproto.codegen;

import javax.tools.*;
import java.net.URI;
import java.util.*;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCompiler {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public static Class<?> compile(String className, String sourceCode)
            throws URISyntaxException, ClassNotFoundException {
        JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));

        List<JavaFileObject> compilationUnits = List.of(
                new SourceFileObject(className, sourceCode));

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
            throw new RuntimeException("Compilation failed");
        }

        // Load the compiled class
        return fileManager.getClassLoader(null).loadClass(className);
    }

    private static class SourceFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        SourceFileObject(String name, String sourceCode) throws URISyntaxException {
            super(new URI("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
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
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
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
