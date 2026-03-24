package src.gui.core;

import java.util.Set;

/**
 * Listener for breakpoint changes in the source editor.
 */
public interface BreakpointListener {
    /**
     * Called when breakpoints are modified.
     *
     * @param lines set of line numbers with active breakpoints
     */
    void onBreakpointsChanged(Set<Integer> lines);
}
