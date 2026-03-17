# Rubric coverage demos (Semantics + Interpreter)

This folder is the “presentation pack”: a small set of Herd programs you can run in the Herd IDE (or via CLI) to demonstrate rubric categories for **semantic analysis** and the **interpreter**.

## Quick start (IDE)

1. Start IDE: `./gradlew runIDE`
2. Open a file: **Ctrl+O**
3. Run: **Ctrl+Enter**
4. Use these tabs:
   - **Semantic**: semantic pass/fail + semantic error list
   - **Output**: interpreter output (only when semantics pass)
   - **Errors**: aggregated errors (lexer/parser/semantic/runtime)

## Recommended demo order (what to run + what to say)

### Demo A — Semantic analysis (multiple errors in one run)

Open: `11_semantic_errors_pack.hd`

What to say (script):
- “This shows the semantic analyzer catching multiple issues in one pass (it doesn’t stop at the first error).”
- “These are *semantic* errors: the program is syntactically valid, but invalid by language rules (name resolution, types, const rules, function call checking).”
- “Notice errors are annotated with line/column and also appear in the Errors tab.”

Rubric mapping (typical errors shown here):
- Undeclared identifier
- Duplicate variable / multiply-defined variable
- Assignment type mismatch (e.g., assigning `bool` to `int`)
- Constant reassignment
- Function call checking (argument count mismatch, argument type mismatch)

Expected result:
- **Semantic** tab shows an error list (more than one error).
- **Output** tab should not show a successful run.

### Demo B — Interpreter success run (core constructs)

Open: `01_interpreter_constructs_ok.hd`

What to say (script):
- “This one should be Semantic OK, then the interpreter executes and prints results.”
- “It covers expressions, control flow, arrays, and functions/recursion in a single run.”
- “The Output tab is the interpreter’s runtime behavior after semantic analysis succeeds.”

Rubric mapping (construct coverage):
- Declarations and assignments (ints/floats/bools/strings/chars)
- Constants (`const ... = literal`)
- Arithmetic + nested expressions (including function calls in expressions)
- Boolean expressions (relational + logical)
- Conditionals (`if/else`)
- Loops (`while`, `for`, `repeat-until`)
- Arrays (fixed size, indexing, assignment)
- Functions + recursion (factorial), variable scope inside blocks
- I/O: `print(...)`

Expected Output (roughly):
- A header line with the demo name and a few values
- `sum(arr)= 35  ok= true`
- `fact( 5 )= 120`
- `repeat-until t= 3`
- `nested ok j= 1`
- `=== done ===`

### Demo C — Interpreter runtime error messaging

Open: `02_interpreter_runtime_error_assert.hd`

What to say (script):
- “This demonstrates the difference between semantic errors vs runtime errors.”
- “Semantic analysis passes, so execution starts; then the interpreter hits an assertion and reports a runtime error.”
- “Runtime errors show in the Errors tab (and usually stop execution).”

Rubric mapping:
- Runtime error detection/reporting (assertion failure)

Expected result:
- **Semantic** tab: OK
- **Errors** tab: runtime error message similar to “Assertion failed”

## Parser-only coverage (if asked about records/pointers)

Open: `20_parser_only_record_and_pointers.hd`

What to say (script):
- “This file is primarily to show that the grammar supports `record` types and pointer type syntax.”
- “Our interpreter focus for this milestone is on the core execution constructs (expressions/control flow/functions/arrays). Record/pointer execution may not be fully implemented in the interpreter pipeline, so this is for parser coverage.”

## CLI fallback (if IDE acts up)

- Semantic-only:
  - `./gradlew -q run --args="--semantic tests/rubric/11_semantic_errors_pack.hd"`
- Interpreter run:
  - `./gradlew -q run --args="--run tests/rubric/01_interpreter_constructs_ok.hd"`
  - `./gradlew -q run --args="--run tests/rubric/02_interpreter_runtime_error_assert.hd"`

## Common gotcha (so your demos don’t fail)

- In Herd, **all variable declarations must appear before any statements** inside a `{ ... }` block.
  - If you put `int x;` after a `print(...)` or assignment, you’ll get a parser error like: `Unexpected statement start: INT`.
