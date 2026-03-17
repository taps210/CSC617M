# Interpreter demo pack (for Herd IDE)

## How to run in the IDE

1. Launch: `./gradlew runIDE`
2. Open a `.hd` file from this folder (Ctrl+O).
3. Press `Ctrl+Enter`.
4. View results in the **Output** tab (interpreter output) and **Errors** tab (runtime errors).

These programs are meant to demonstrate interpreter behavior *after* semantic analysis succeeds.

## Rubric mapping

- Expressions + loops + functions + recursion + arrays + print: `tests/rubric/01_interpreter_constructs_ok.hd`
- Runtime error messaging: `tests/rubric/02_interpreter_runtime_error_assert.hd`
