package src.gui.core;

/**
 * Listener for live runtime execution events used by the IDE Output panel.
 */
public interface RuntimeEventListener {
    default void onRuntimeStarted() {}
    default void onRuntimeOutput(String text) {}
    default void onRuntimeFinished(String finalOutput, String runtimeError) {}
}
