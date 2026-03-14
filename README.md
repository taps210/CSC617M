# Herd Programming Language

Requires Java 17.

---

## Build

```bash
./gradlew build
```

Windows: `gradlew.bat build`

To compile manually without Gradle (classpath must point to `build/classes/java/main`):

```bash
javac -d build/classes/java/main src/src/*.java
```

---

## Gradle tasks

| Task | Description |
|------|-------------|
| `./gradlew build` | Compile and assemble the project. |
| `./gradlew clean` | Delete the `build/` directory. |
| `./gradlew test` | Run all JUnit tests. |
| `./gradlew run --args="..."` | Run the CLI (see modes below). |
| `./gradlew runIDE` | Launch the graphical Herd IDE. |

Test reports are written to `build/reports/tests/test/index.html`.

---

## CLI modes

Run from the **project root** (where `build.gradle.kts` is), after building.

### Scan
Tokenize the source file and print the token stream.
```bash
./gradlew run --args="--scan <inputFile>"
./gradlew run --args="--scan --out <outputFile> <inputFile>"
```

### Parse
Scan + parse; prints the parse tree.
```bash
./gradlew run --args="--parse <inputFile>"
./gradlew run --args="--parse --out <outputFile> <inputFile>"
```

### Semantic analysis
Scan + parse + semantic analysis; reports errors or confirms success.
```bash
./gradlew run --args="--semantic <inputFile>"
./gradlew run --args="--semantic --out <outputFile> <inputFile>"
```

### IR (intermediate representation)
Scan + parse + semantic + IR generation; prints three-address code for all functions.
```bash
./gradlew run --args="--ir <inputFile>"
./gradlew run --args="--ir --out <outputFile> <inputFile>"
```

### CFG (control flow graph)
Same as `--ir`, then builds basic blocks and prints the CFG with successor sets.
```bash
./gradlew run --args="--cfg <inputFile>"
./gradlew run --args="--cfg --out <outputFile> <inputFile>"
```

### Run (interpreter)
Full pipeline: scan → parse → semantic → IR → execute with the interpreter.
```bash
./gradlew run --args="--run <inputFile>"
./gradlew run --args="--run --out <outputFile> <inputFile>"
```

### Benchmark
Scan-only timing benchmark (no `--out`).
```bash
./gradlew run --args="--bench <inputFile>"
```

---

## Direct `java` invocation

After `./gradlew build`, you can also run the CLI directly:

```bash
java -cp build/classes/java/main src.Main --scan <inputFile>
java -cp build/classes/java/main src.Main --scan --out <outputFile> <inputFile>

java -cp build/classes/java/main src.Main --parse <inputFile>
java -cp build/classes/java/main src.Main --parse --out <outputFile> <inputFile>

java -cp build/classes/java/main src.Main --semantic <inputFile>
java -cp build/classes/java/main src.Main --semantic --out <outputFile> <inputFile>

java -cp build/classes/java/main src.Main --ir <inputFile>
java -cp build/classes/java/main src.Main --ir --out <outputFile> <inputFile>

java -cp build/classes/java/main src.Main --cfg <inputFile>
java -cp build/classes/java/main src.Main --cfg --out <outputFile> <inputFile>

java -cp build/classes/java/main src.Main --run <inputFile>

java -cp build/classes/java/main src.Main --bench <inputFile>
```

If you see "Could not find or load main class src.Main", make sure you are in the project root and have run `./gradlew build` first.

---

## Herd IDE

Graphical IDE with editor, compile, AST/parse tree tabs, IR output, and error display:

```bash
./gradlew runIDE
```

Windows: `gradlew.bat runIDE`

---

## Scanner visualization (Streamlit)

One-page dashboard: token list, large-file performance, and sample programs.

```bash
pip install -r viz/requirements.txt
streamlit run viz/app.py
```

Requires the project to be built (`./gradlew build`) and large files to exist (`python3 scripts/generate_large_files.py` if needed).
