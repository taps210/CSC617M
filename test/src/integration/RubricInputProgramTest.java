package src.integration;

import org.junit.jupiter.api.Test;
import src.Ast.ProgramNode;
import src.Parser;
import src.Scanner;
import src.Token;
import src.errors.SemanticError;
import src.ir.FunctionIR;
import src.ir.IrBuilder;
import src.ir.IrInterpreter;
import src.semantic.SemanticAnalyzer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubricInputProgramTest {

    @Test
    void complexRubricProgram_consumesInput_andProducesExpectedOutput() throws Exception {
        Path programPath = Path.of("tests/rubric/03_interpreter_inputs_complex.hd");
        Path inputPath = Path.of("tests/rubric/03_interpreter_inputs_complex.in");
        Path expectedPath = Path.of("tests/rubric/03_interpreter_inputs_complex.out");

        String source = Files.readString(programPath);
        String input = Files.readString(inputPath);
        String expected = Files.readString(expectedPath);

        List<Token> tokens = new Scanner(source).tokenizeAll();
        Parser parser = new Parser(tokens, new StringBuilder());
        ProgramNode ast = parser.parseProgramToAst();

        List<SemanticError> semErrors = SemanticAnalyzer.analyze(ast);
        assertTrue(semErrors.isEmpty(), "Program should be semantically valid for runtime test");

        List<FunctionIR> funcs = IrBuilder.buildProgram(ast);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);

        new IrInterpreter(funcs, ast, in, out).run();

        String actual = normalize(outBytes.toString(StandardCharsets.UTF_8));
        assertEquals(normalize(expected), actual);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n').trim();
    }
}
