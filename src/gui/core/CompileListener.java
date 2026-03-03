package src.gui.core;

/**
 * Interface for panels to receive compile results.
 * Panels never import Scanner or Parser; they only react to CompileResult.
 */
public interface CompileListener {
    void onCompileComplete(CompileResult result);
}
