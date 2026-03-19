package src.ir;

import org.junit.jupiter.api.Test;
import src.Ast.ProgramNode;
import src.Parser;
import src.Scanner;
import src.Token;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IrInterpreterInputTest {

    @Test
    void readConsumesMultipleInputLines() {
        String source = "void main() { int a; int b; read(a); read(b); print(a, b); }";
        List<Token> tokens = new Scanner(source).tokenizeAll();

        Parser parser = new Parser(tokens, new StringBuilder());
        parser.parseProgram();
        ProgramNode ast = parser.getProgramNode();
        assertNotNull(ast);

        List<FunctionIR> functions = IrBuilder.buildProgram(ast);

        ByteArrayInputStream in = new ByteArrayInputStream("10\n20\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);

        new IrInterpreter(functions, ast, in, out).run();

        assertEquals("10 20", outBytes.toString(StandardCharsets.UTF_8).trim());
    }
}
