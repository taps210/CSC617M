package src.integration;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class AbmProgramTest extends ScannerTestBase {

    @Test
    void agentWorldSpawn_snippet() {
        assertTokenTypes(tokenize("agent world spawn move step neighbors rand self assert"),
                TokenType.AGENT, TokenType.WORLD, TokenType.SPAWN, TokenType.MOVE, TokenType.STEP,
                TokenType.NEIGHBORS, TokenType.RAND, TokenType.SELF, TokenType.ASSERT, TokenType.EOF);
    }

    @Test
    void agentDeclaration_withFields() {
        String src = "agent Player { int x; int y; }";
        assertTokenTypes(tokenize(src),
                TokenType.AGENT, TokenType.IDENT, TokenType.LBRACE,
                TokenType.INT, TokenType.IDENT, TokenType.SEMI,
                TokenType.INT, TokenType.IDENT, TokenType.SEMI,
                TokenType.RBRACE, TokenType.EOF);
    }

    @Test
    void mainWithSpawnAndStep() {
        String src = "void main() { spawn Player(0, 0); step(); }";
        assertTokenTypes(tokenize(src),
                TokenType.VOID, TokenType.MAIN, TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE,
                TokenType.SPAWN, TokenType.IDENT, TokenType.LPAREN, TokenType.INT_LIT, TokenType.COMMA, TokenType.INT_LIT, TokenType.RPAREN, TokenType.SEMI,
                TokenType.STEP, TokenType.LPAREN, TokenType.RPAREN, TokenType.SEMI,
                TokenType.RBRACE, TokenType.EOF);
    }
}
