package src.literals;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BooleansTest extends ScannerTestBase {

    @Test
    void true_literal() {
        List<Token> tokens = tokenize("true");
        assertTokenTypes(tokens, TokenType.TRUE, TokenType.EOF);
    }

    @Test
    void false_literal() {
        List<Token> tokens = tokenize("false");
        assertTokenTypes(tokens, TokenType.FALSE, TokenType.EOF);
    }

    @Test
    void trueAndFalse() {
        List<Token> tokens = tokenize("true false");
        assertTokenTypes(tokens, TokenType.TRUE, TokenType.FALSE, TokenType.EOF);
    }
}
