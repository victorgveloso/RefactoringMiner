package extension.base.factory;

import extension.base.lang.python.Python3Parser;
import org.antlr.v4.runtime.*;

import java.util.Map;
import java.util.function.Function;

import static extension.base.factory.LangLexerFactory.getLexer;

public class LangParserFactory {

    private static final Map<String, Function<TokenStream, Parser>> PARSER_FACTORIES = Map.of(
            "python", Python3Parser::new,
            "py", Python3Parser::new
            // Add new languages in the future
    );


    public static Parser getParser(String language, TokenStream tokenStream) {
        Function<TokenStream, Parser> parserFactory = PARSER_FACTORIES.get(language.toLowerCase());
        if (parserFactory == null) {
            throw new IllegalArgumentException("No parser available for language: " + language);
        }

        return parserFactory.apply(tokenStream);
    }


    public static Parser getParserForCode(String language, String code) {
        CharStream input = CharStreams.fromString(code);
        Lexer lexer = getLexer(language, input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return getParser(language, tokens);
    }

    public static Parser getParserFromLexer(String language, Lexer lexer) {
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return getParser(language, tokens);
    }
}
