package src.errors;

/**
 * One lexical error (line, column, message). Used to build the list of scanner errors
 * for the GUI and other consumers. The scanner adds these to a list when it recovers
 * from recoverable errors, or includes one in a thrown {@link ScanAbortedException} when
 * it cannot continue (e.g. unterminated string).
 */
public record LexicalErrorRecord(int line, int col, String message) {

    /** Format for text output (e.g. error report). */
    public String format() {
        return "Error found in line %d column %d%n%s".formatted(line, col, message);
    }

    /**
     * Thrown when the scanner hits an error and stops. If a list was passed to
     * tokenizeAll, the error(s) are already in that list and this carries none.
     * Otherwise this can carry the single error via {@link #getError()}.
     */
    public static final class ScanAbortedException extends RuntimeException {
        private final LexicalErrorRecord error;

        /** Use when errors were already added to the caller's list. */
        public ScanAbortedException() {
            this.error = null;
        }

        /** Use when no list was passed; the single error is stored here. */
        public ScanAbortedException(LexicalErrorRecord error) {
            this.error = error;
        }

        public LexicalErrorRecord getError() {
            return error;
        }

        @Override
        public String getMessage() {
            return error != null ? error.format() : "Lexical error(s) occurred";
        }
    }
}
