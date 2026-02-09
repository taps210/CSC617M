# CSC617M Scanner

Requires Java 17.

**Build**

```bash
./gradlew build
```

On Windows: `gradlew.bat build`

**Run**

After building:

```bash
java -cp build/classes/java/main src.Main --print <inputFile>
java -cp build/classes/java/main src.Main --out <outputFile> <inputFile>
java -cp build/classes/java/main src.Main --bench <inputFile>
java -cp build/classes/java/main src.Main --collect-errors <inputFile> [outputFile]
```

**Tests**

```bash
./gradlew test
```

Reports under `build/reports/tests/test/index.html` (open in a browser). Same `gradlew.bat` note on Windows.
