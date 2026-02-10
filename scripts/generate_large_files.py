#!/usr/bin/env python3
"""
Generate LargeFile_Small.txt (~1/3 size) and LargeFile_Large.txt (2x size)
from tests/inputs/LargeFile.txt. Run from project root.

After running, update bench stats by running:
  java -cp build/classes/java/main src.Main --bench tests/inputs/LargeFile_Small.txt
  java -cp build/classes/java/main src.Main --bench tests/inputs/LargeFile_Large.txt
Then create tests/outputs/LargeFile_Small_BenchStats.txt and LargeFile_Large_BenchStats.txt
with the same format as LargeFile_BenchStats.txt.
"""
import os
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
INPUTS = PROJECT_ROOT / "tests" / "inputs"
OUTPUTS = PROJECT_ROOT / "tests" / "outputs"
LARGE = INPUTS / "LargeFile.txt"
SMALL = INPUTS / "LargeFile_Small.txt"
LARGE_2X = INPUTS / "LargeFile_Large.txt"


def main() -> None:
    if not LARGE.exists():
        raise SystemExit(f"Missing {LARGE}")
    content = LARGE.read_bytes()
    n = len(content)

    # Small: first 1/3
    small_content = content[: n // 3]
    SMALL.write_bytes(small_content)
    print(f"Wrote {SMALL} ({len(small_content):,} bytes)")

    # Large: 2x (concatenate full content twice)
    large_content = content + content
    LARGE_2X.write_bytes(large_content)
    print(f"Wrote {LARGE_2X} ({len(large_content):,} bytes)")

    print("Run scanner --bench on each file and create *_BenchStats.txt in tests/outputs/")


if __name__ == "__main__":
    main()
