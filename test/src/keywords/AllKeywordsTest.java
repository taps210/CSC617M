package src.keywords;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllKeywordsTest extends ScannerTestBase {

    @Test
    void declarations_useConstTypeRecordAgentWorld() {
        assertTokenTypes(tokenize("use const type record agent world"),
                TokenType.USE, TokenType.CONST, TokenType.TYPE, TokenType.RECORD, TokenType.AGENT, TokenType.WORLD, TokenType.EOF);
    }

    @Test
    void types_intFloatCharStringBoolVoid() {
        assertTokenTypes(tokenize("int float char string bool void"),
                TokenType.INT, TokenType.FLOAT, TokenType.CHAR, TokenType.STRING, TokenType.BOOL, TokenType.VOID, TokenType.EOF);
    }

    @Test
    void controlFlow_ifElseWhileForRepeatUntilReturnBreakContinue() {
        assertTokenTypes(tokenize("if else while for repeat until return break continue"),
                TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR, TokenType.REPEAT, TokenType.UNTIL,
                TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE, TokenType.EOF);
    }

    @Test
    void io_readPrint() {
        assertTokenTypes(tokenize("read print"), TokenType.READ, TokenType.PRINT, TokenType.EOF);
    }

    @Test
    void booleans_trueFalse() {
        assertTokenTypes(tokenize("true false"), TokenType.TRUE, TokenType.FALSE, TokenType.EOF);
    }

    @Test
    void main_keyword() {
        assertTokenTypes(tokenize("main"), TokenType.MAIN, TokenType.EOF);
    }

    @Test
    void abm_spawnMoveStepNeighborsRand() {
        assertTokenTypes(tokenize("spawn move step neighbors rand"),
                TokenType.SPAWN, TokenType.MOVE, TokenType.STEP, TokenType.NEIGHBORS, TokenType.RAND, TokenType.EOF);
    }

    @Test
    void self_keyword() {
        assertTokenTypes(tokenize("self"), TokenType.SELF, TokenType.EOF);
    }

    @Test
    void assert_keyword() {
        assertTokenTypes(tokenize("assert"), TokenType.ASSERT, TokenType.EOF);
    }

    @Test
    void allKeywords_singleSequence() {
        List<Token> tokens = tokenize("use const type record agent world int float char string bool void " +
                "if else while for repeat until return break continue read print true false main " +
                "spawn move step neighbors rand self assert");
        TokenType[] expected = {
                TokenType.USE, TokenType.CONST, TokenType.TYPE, TokenType.RECORD, TokenType.AGENT, TokenType.WORLD,
                TokenType.INT, TokenType.FLOAT, TokenType.CHAR, TokenType.STRING, TokenType.BOOL, TokenType.VOID,
                TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR, TokenType.REPEAT, TokenType.UNTIL,
                TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE, TokenType.READ, TokenType.PRINT,
                TokenType.TRUE, TokenType.FALSE, TokenType.MAIN,
                TokenType.SPAWN, TokenType.MOVE, TokenType.STEP, TokenType.NEIGHBORS, TokenType.RAND,
                TokenType.SELF, TokenType.ASSERT, TokenType.EOF
        };
        assertTokenTypes(tokens, expected);
    }

    @Test
    void caseSensitive_IntIsIdentifier() {
        List<Token> tokens = tokenize("Int");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("Int", tokens.get(0).lexeme());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void caseSensitive_FloatIsIdentifier() {
        List<Token> tokens = tokenize("Float");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("Float", tokens.get(0).lexeme());
    }

    @Test
    void caseSensitive_TrueIsIdentifier() {
        List<Token> tokens = tokenize("True");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("True", tokens.get(0).lexeme());
    }

    @Test
    void caseSensitive_IFIsIdentifier() {
        List<Token> tokens = tokenize("IF");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("IF", tokens.get(0).lexeme());
    }
}
