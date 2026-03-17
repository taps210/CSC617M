# Semantic test pack (for Herd IDE)

## How to run in the IDE

1. Launch the IDE: `./gradlew runIDE`
2. Open any `.hd` file from this folder (File tab → Open, or Ctrl+O).
3. Press `Ctrl+Enter` to compile/run.
4. Click the **Semantic** output tab:
   - Passing tests should show **"Semantic: OK"** and "No semantic errors found."
   - Failing tests should show semantic errors (also listed in the **Errors** tab).

These files are meant to validate the semantic analyzer’s checks (name resolution, duplicates, context rules like `self`/`move`, method existence, and return types).

## Rubric mapping (quick demos)

- Undeclared variable / identifier: `02_err_undefined_identifier.hd`
- Multiply-defined variable: `03_err_duplicate_variable.hd`
- Type mismatch + const reassignment + parameter mismatch: `tests/rubric/11_semantic_errors_pack.hd`
