package extension.base.factory;

import extension.base.lang.python.Python3Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

import java.util.Map;
import java.util.function.Function;

public class LangLexerFactory {
    public static Lexer getLexerForCode(String language, String code) {
        CharStream input = CharStreams.fromString(code);
        return getLexer(language, input);
    }

    public static Lexer getLexer(String language, CharStream input) {
        Function<CharStream, Lexer> lexerFactory = LEXER_FACTORIES.get(language.toLowerCase());
        if (lexerFactory == null) {
            throw new IllegalArgumentException("No lexer available for language: " + language);
        }

        return lexerFactory.apply(input);
    }

    private static final Map<String, Function<CharStream, Lexer>> LEXER_FACTORIES = Map.of(
            "python", Python3Lexer::new,
            "py", Python3Lexer::new
            // Add new languages in the future
    );
}
