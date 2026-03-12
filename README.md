# CSC617M Scanner

Requires Java 17.

**Build**

```bash
./gradlew build
```

On Windows: `gradlew.bat build`

Or with `java` directly (classpath must point to `build/classes/java/main`):

```bash
javac -d build/classes/java/main src\src\*.java
```

**Run**

From the **project root** (where `build.gradle.kts` is), after building:

```bash
# Scan only (print tokens to stdout)
./gradlew run --args="--scan tests/inputs/Sample01_Marketplace.txt"

# Parse (scan + parse, trace to stdout)
./gradlew run --args="--parse tests/inputs/Sample01_Marketplace.txt"

# Semantic (scan + parse + semantic analysis)
./gradlew run --args="--semantic tests/inputs/Sample01_Marketplace.txt"
```

Or with `java` directly (classpath must point to `build/classes/java/main`):

```bash
# Scan
java -cp build/classes/java/main src.Main --scan <inputFile>
java -cp build/classes/java/main src.Main --scan --out <outputFile> <inputFile>

# Parse
java -cp build/classes/java/main src.Main --parse <inputFile>
java -cp build/classes/java/main src.Main --parse --out <outputFile> <inputFile>

# Semantic
java -cp build/classes/java/main src.Main --semantic <inputFile>
java -cp build/classes/java/main src.Main --semantic --out <outputFile> <inputFile>

# Benchmark (scan only)
java -cp build/classes/java/main src.Main --bench <inputFile>
```

If you see "Could not find or load main class src.Main", run from the project root and run `./gradlew build` first.

**Herd IDE**

Run the graphical IDE (editor, compile, AST/parse tree tabs, errors):

```bash
./gradlew runIDE
```

On Windows: `gradlew.bat runIDE`

**Tests**

```bash
./gradlew test
```

Reports under `build/reports/tests/test/index.html` (open in a browser). Same `gradlew.bat` note on Windows.

**Scanner visualization (Streamlit)**

One-page dashboard: token list, large-file performance, and the five sample programs (code, scanner output, description). Sample 2 shows both the neighbors and zone variants.

From project root:

```bash
pip install -r viz/requirements.txt
streamlit run viz/app.py
```

Requires the project to be built (`./gradlew build`) and the large files to exist (`python3 scripts/generate_large_files.py` if needed).
