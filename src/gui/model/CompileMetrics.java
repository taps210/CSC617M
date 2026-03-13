package src.gui.model;

/**
 * Data class for all timing and count stats from source, scanner, and parser.
 */
public class CompileMetrics {
    // Source
    public int totalLines;
    public int codeLines;
    public int blankLines;
    public int commentLines;
    public int charCount;
    public int wordCount;

    // Scanner
    public int totalTokens;
    public int keywordCount;
    public int identifierCount;
    public int literalCount;
    public int operatorCount;
    public int commentCount;
    public int scanErrorCount;
    public long scanTimeNs;

    // Parser
    public int parseNodeCount;  // used as construct count (emit() calls)
    public int parseErrorCount;
    public int parseWarningCount;
    public long parseTimeNs;

    // IR
    public int irInstrCount;
    public long irTimeNs;

    // Run
    public long runTimeNs;
}
