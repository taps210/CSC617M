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
./gradlew run --args="--print tests/inputs/Sample01_Marketplace.txt"
```

Or with `java` directly (classpath must point to `build/classes/java/main`):

```bash
java -cp build/classes/java/main src.Main --print tests/inputs/Sample01_Marketplace.txt
java -cp build/classes/java/main src.Main --out <outputFile> <inputFile>
java -cp build/classes/java/main src.Main --bench <inputFile>
java -cp build/classes/java/main src.Main --collect-errors <inputFile> [outputFile]
```

If you see "Could not find or load main class src.Main", run from the project root and run `./gradlew build` first.

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
