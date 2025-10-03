package org.refactoringminer.utils;


import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.stringifier.PyASTFlattener;
import extension.base.lang.csharp.CSharpLexer;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.python.Python3Lexer;
import extension.base.lang.python.Python3Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class LangASTUtil {

    public static void printAST(String code) {
        // Parse the code using ANTLR

        Python3Lexer lexer = new Python3Lexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
        Python3Parser.File_inputContext parseTree = parser.file_input();  // Generate the parse tree

        // Build the AST using the PythonASTBuilder
        PyASTBuilder astBuilder = new PyASTBuilder();
        LangASTNode ast = astBuilder.build(parseTree);  // Build the AST

        // Debug: Print the parse tree for reference
        System.out.println("Parse Tree:");
        System.out.println(parseTree.toStringTree(parser));

        System.out.println("\nAST Structure:");
        System.out.println(ast.toString());
    }

    public static void printAST(Path pythonFilePath) throws IOException {
        // Parse the code using ANTLR

        CharStream charStream = CharStreams.fromPath(pythonFilePath);
        Python3Lexer lexer = new Python3Lexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
        Python3Parser.File_inputContext parseTree = parser.file_input();  // Generate the parse tree

        // Build the AST using the PythonASTBuilder
        PyASTBuilder astBuilder = new PyASTBuilder();
        LangASTNode ast = astBuilder.build(parseTree);  // Build the AST

        // Debug: Print the parse tree for reference
        System.out.println("Parse Tree:");
        System.out.println(parseTree.toStringTree(parser));

        // Print the AST using the ASTPrinter
//        System.out.println("\nAST Structure:");
//        LangASTPrinter printer = new LangASTPrinter();
//        ast.accept(printer);
    }

    public static void printASTCSharp(String code) {
        // Parse the C# code using ANTLR
        CSharpLexer lexer = new CSharpLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);
        CSharpParser.Compilation_unitContext parseTree = parser.compilation_unit();  // Generate the parse tree

        // Build the AST using the CSharpASTBuilder
        CSharpASTBuilder astBuilder = new CSharpASTBuilder();
        LangASTNode ast = astBuilder.build(parseTree);  // Build the AST

        // Debug: Print the parse tree for reference
        System.out.println("Parse Tree:");
        System.out.println(parseTree.toStringTree(parser));

        System.out.println("\nAST Structure:");
        System.out.println(ast.toString());
    }

    public static LangASTNode parsePythonCodeToAST(String code) {
        // Use the Python lexer and parser to parse the code
        Python3Lexer lexer = new Python3Lexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
        Python3Parser.File_inputContext fileInputContext = parser.file_input();

        // Build our AST using the PyASTBuilder
        PyASTBuilder pyASTBuilder = new PyASTBuilder();
        return pyASTBuilder.build(fileInputContext);
    }

    public static String getPyStringify(String code){
        LangASTNode ast = LangASTUtil.parsePythonCodeToAST(code);

        // Stringify AST
        return PyASTFlattener.flattenNode(ast);
    }

    /**
     * Reads a resource file from various possible locations.
     *
     * @param resourcePath The path to the resource file
     * @return The contents of the resource file as a string
     * @throws IOException If the resource file cannot be found or read
     */
    public static String readResourceFile(String resourcePath) throws IOException {
        InputStream is = null;
        List<String> attemptedPaths = new ArrayList<>();

        // Try multiple ways to load the resource

        // 1. Try with classloader from Thread context - most reliable across environments
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        is = contextClassLoader.getResourceAsStream(resourcePath);
        attemptedPaths.add("Thread.currentThread().getContextClassLoader().getResourceAsStream(\"" + resourcePath + "\")");

        // 2. Try with a leading slash for absolute path from classpath root
        if (is == null) {
            is = LangASTUtil.class.getResourceAsStream("/" + resourcePath);
            attemptedPaths.add("LangASTUtil.class.getResourceAsStream(\"/\" + resourcePath)");
        }

        // 3. Try without the leading slash (relative to the package)
        if (is == null) {
            is = LangASTUtil.class.getResourceAsStream(resourcePath);
            attemptedPaths.add("LangASTUtil.class.getResourceAsStream(resourcePath)");
        }

        // 4. Try with ClassLoader from LangASTUtil
        if (is == null) {
            is = LangASTUtil.class.getClassLoader().getResourceAsStream(resourcePath);
            attemptedPaths.add("LangASTUtil.class.getClassLoader().getResourceAsStream(resourcePath)");
        }

        // 5. Try with system classloader
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream(resourcePath);
            attemptedPaths.add("ClassLoader.getSystemResourceAsStream(resourcePath)");
        }

        // 6. If still not found, try with direct file access as a fallback
        if (is == null) {
            try {
                Path filePath = Paths.get(System.getProperty("user.dir"), "src/test/resources", resourcePath);
                if (Files.exists(filePath)) {
                    is = Files.newInputStream(filePath);
                    attemptedPaths.add("Direct file access: " + filePath);
                }
            } catch (Exception e) {
                // Ignore, we'll handle the null InputStream below
            }
        }

        // If all attempts failed, provide detailed debugging information
        if (is == null) {
            System.err.println("❌ Resource not found: " + resourcePath);
            System.err.println("Current working directory: " + System.getProperty("user.dir"));
            System.err.println("Current class: " + LangASTUtil.class.getName());
            System.err.println("Attempted to load from:");
            for (String path : attemptedPaths) {
                System.err.println(" - " + path);
            }

            // Debug: list test resources directory contents
            try {
                Path resourcesDir = Paths.get(System.getProperty("user.dir"), "src/test/resources");
                if (Files.exists(resourcesDir)) {
                    System.err.println("\nTest resources directory exists. Contents:");
                    Files.walk(resourcesDir, 3)
                            .forEach(p -> System.err.println(" - " + resourcesDir.relativize(p)));
                } else {
                    System.err.println("\nTest resources directory does not exist at: " + resourcesDir);
                }
            } catch (Exception e) {
                System.err.println("Error listing resources directory: " + e.getMessage());
            }

            throw new IOException("Resource not found: " + resourcePath);
        }

        // Read the content
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            is.close();
        }
    }




}
