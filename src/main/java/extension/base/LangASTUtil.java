package extension.base;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.python.Python3Lexer;
import extension.base.lang.python.Python3Parser;
import extension.base.treesitter.python.Printer;
import extension.base.treesitter.python.PyTSBuilder;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.treesitter.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Utility class to create Language AST parsers
 * based on file extensions and supported programming languages
 */
public class LangASTUtil {


    public static LangASTNode getLangAST(LangSupportedEnum language, String content) throws IOException {

        if (language == null) {
            throw new UnsupportedOperationException("Language not supported for file");
        }

        switch (language) {
            case PYTHON:
//                return getTreeSitterPythonAST(content);
                return getCustomPythonAST(new StringReader(content));
            case CSHARP:
                return getCustomCSharpAST(new StringReader(content));
            default:
                throw new UnsupportedOperationException("Parser not implemented for language: " + language);
        }

    }

    public static PyTSBuilder getTreeSitterPythonAST(String r,  Printer printer) throws IOException {
        TSNode rootNode = prepareTSNodeForTreeSitterPythonAST(r);
        // Build custom AST
        PyTSBuilder astBuilder = new PyTSBuilder(rootNode, printer);

        return astBuilder;
    }

    public static TSNode prepareTSNodeForTreeSitterPythonAST(String r) throws IOException {
        TSParser parser = new TSParser();
        // 0.set language parser
        parser.setLanguage(new TreeSitterPython());
        // 1.parser with string input
        TSTree tree = parser.parseString(null, r);
        // 2.traverse the AST tree with DOM like APIs
        TSNode rootNode = tree.getRootNode();
        // debug the parser with a logger
        TSLogger logger = (type, message) -> {
            System.out.println(message);
        };
        parser.setLogger(logger);
        // or output the AST tree as DOT graph
        File dotFile = File.createTempFile("python", ".dot");
        parser.printDotGraphs(dotFile);
        return rootNode;
    }

    public static PyTSBuilder getTreeSitterPythonAST(String r) throws IOException {
        TSNode rootNode = prepareTSNodeForTreeSitterPythonAST(r);
        // Build custom AST
        PyTSBuilder astBuilder = new PyTSBuilder(rootNode);

        return astBuilder;
    }

    public static LangASTNode getCustomPythonAST(Reader r) throws IOException {

        // Parse the Python code
        CharStream input = CharStreams.fromReader(r);
        Python3Lexer lexer = new Python3Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);

        // Get the parse tree
        Python3Parser.File_inputContext parseTree = parser.file_input();

        // Build custom AST
        PyASTBuilder astBuilder = new PyASTBuilder();

        return astBuilder.build(parseTree);
    }


    public static LangASTNode getCustomCSharpAST(Reader r) throws IOException {

        // Parse the C# code
        CharStream input = CharStreams.fromReader(r);
        extension.base.lang.csharp.CSharpLexer lexer = new extension.base.lang.csharp.CSharpLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);

        // Get the parse tree
        CSharpParser.Compilation_unitContext parseTree = parser.compilation_unit();

        // Build custom AST
        CSharpASTBuilder astBuilder = new CSharpASTBuilder();

        return astBuilder.build(parseTree);
    }

}
