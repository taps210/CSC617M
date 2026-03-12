package src.gui.core;

import src.errors.LexicalErrorRecord;
import src.errors.ParseException;
import src.errors.SemanticError;
import src.Parser;
import src.Scanner;
import src.Token;
import src.TokenType;
import static src.Ast.*;
import src.gui.model.CompileError;
import src.parsetree.ParseTreeNode;
import src.gui.model.CompileMetrics;
import src.semantic.SemanticAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Single compile authority. Runs scanner and parser, measures timing,
 * builds CompileResult and CompileMetrics, notifies listeners.
 */
public class CompileController {
    private static final Set<TokenType> KEYWORD_TYPES = Set.of(
            TokenType.USE, TokenType.CONST, TokenType.TYPE, TokenType.RECORD, TokenType.AGENT, TokenType.WORLD,
            TokenType.INT, TokenType.FLOAT, TokenType.CHAR, TokenType.STRING, TokenType.BOOL, TokenType.VOID,
            TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR, TokenType.REPEAT, TokenType.UNTIL,
            TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE,
            TokenType.READ, TokenType.PRINT,
            TokenType.TRUE, TokenType.FALSE, TokenType.MAIN,
            TokenType.SPAWN, TokenType.MOVE, TokenType.STEP, TokenType.NEIGHBORS, TokenType.RAND,
            TokenType.UPDATE, TokenType.DESTROY, TokenType.ZONE, TokenType.PRE, TokenType.POST,
            TokenType.SELF, TokenType.NULL, TokenType.ASSERT
    );

    private static final Set<TokenType> LITERAL_TYPES = Set.of(
            TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.STRING_LIT, TokenType.CHAR_LIT
    );

    private static final Set<TokenType> OPERATOR_TYPES = Set.of(
            TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.MOD,
            TokenType.AMP, TokenType.ASSIGN,
            TokenType.EQEQ, TokenType.NEQ, TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE,
            TokenType.ANDAND, TokenType.OROR, TokenType.NOT,
            TokenType.QMARK, TokenType.COLON
    );

    private CompileResult lastResult;
    private final List<CompileListener> listeners = new ArrayList<>();

    public void addListener(CompileListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CompileListener listener) {
        listeners.remove(listener);
    }

    public CompileResult getLastResult() {
        return lastResult;
    }

    public void compile(String sourceText) {
        CompileMetrics metrics = new CompileMetrics();
        computeSourceMetrics(sourceText, metrics);

        List<CompileError> allErrors = new ArrayList<>();
        List<Token> tokens = new ArrayList<>();

        // Scan
        long t0 = System.nanoTime();
        List<LexicalErrorRecord> lexErrors = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(sourceText);
            tokens = scanner.tokenizeAll(false, lexErrors);
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            if (e.getError() != null) lexErrors.add(e.getError());
        }
        long t1 = System.nanoTime();
        metrics.scanTimeNs = t1 - t0;
        metrics.totalTokens = tokens.size();
        metrics.scanErrorCount = lexErrors.size();
        countTokenCategories(tokens, metrics);

        for (LexicalErrorRecord r : lexErrors) {
            allErrors.add(new CompileError(r.line(), r.col(), r.message(), CompileError.Source.LEXER, CompileError.Severity.ERROR));
        }

        String parserTrace = "";
        Optional<ProgramNode> ast = Optional.empty();
        Optional<ParseTreeNode> parseTree = Optional.empty();
        if (!tokens.isEmpty()) {
            StringBuilder trace = new StringBuilder();
            long p0 = System.nanoTime();
            Parser parser = new Parser(tokens, trace);
            try {
                parser.parseProgram();
                trace.append("Parse OK").append(System.lineSeparator());
                ast = Optional.ofNullable(parser.getProgramNode());
                parseTree = Optional.ofNullable(parser.getParseTreeRoot());
            } catch (ParseException e) {
                allErrors.add(new CompileError(e.line, e.col, e.getMessage(), CompileError.Source.PARSER, CompileError.Severity.ERROR));
                trace.append("Parse error: ").append(e.getMessage()).append(System.lineSeparator());
            }
            long p1 = System.nanoTime();
            metrics.parseTimeNs = p1 - p0;
            metrics.parseNodeCount = parser.getConstructCount();
            metrics.parseErrorCount = (int) allErrors.stream().filter(err -> err.source() == CompileError.Source.PARSER).count();
            parserTrace = trace.toString();

            // Semantic analysis after successful parse when AST is present
            if (ast.isPresent()) {
                List<SemanticError> semanticErrors = new SemanticAnalyzer().analyze(ast.get());
                for (SemanticError se : semanticErrors) {
                    allErrors.add(se.toCompileError());
                }
            }
        }

        metrics.parseWarningCount = 0; // v1: no warnings from parser

        CompileResult result = new CompileResult(sourceText, tokens, parserTrace, allErrors, metrics, ast, parseTree);
        this.lastResult = result;
        for (CompileListener l : listeners) {
            l.onCompileComplete(result);
        }
    }

    private void computeSourceMetrics(String sourceText, CompileMetrics metrics) {
        if (sourceText == null || sourceText.isEmpty()) {
            metrics.totalLines = 0;
            metrics.codeLines = 0;
            metrics.blankLines = 0;
            metrics.commentLines = 0;
            metrics.charCount = 0;
            metrics.wordCount = 0;
            return;
        }
        metrics.charCount = sourceText.length();
        String[] words = sourceText.split("\\s+", -1);
        int wordCount = 0;
        for (String w : words) {
            if (!w.isEmpty()) wordCount++;
        }
        metrics.wordCount = wordCount;
        String[] lines = sourceText.split("\n", -1);
        metrics.totalLines = lines.length;
        int blank = 0;
        int comment = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) blank++;
            else if (t.startsWith("//")) comment++;
            // block comment lines not tracked for simplicity
        }
        metrics.blankLines = blank;
        metrics.commentLines = comment;
        metrics.codeLines = metrics.totalLines - blank - comment;
    }

    private void countTokenCategories(List<Token> tokens, CompileMetrics metrics) {
        int keywords = 0, idents = 0, literals = 0, operators = 0, comments = 0;
        for (Token t : tokens) {
            if (t.type() == TokenType.EOF) continue;
            if (KEYWORD_TYPES.contains(t.type())) keywords++;
            else if (t.type() == TokenType.IDENT) idents++;
            else if (LITERAL_TYPES.contains(t.type())) literals++;
            else if (OPERATOR_TYPES.contains(t.type())) operators++;
            // comment tokens not produced by scanner
        }
        metrics.keywordCount = keywords;
        metrics.identifierCount = idents;
        metrics.literalCount = literals;
        metrics.operatorCount = operators;
        metrics.commentCount = comments;
    }
}
