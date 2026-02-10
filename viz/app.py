"""
CSC617M Scanner visualization: token list, large-file performance, and sample programs.
Run from project root: streamlit run viz/app.py
"""
from pathlib import Path
import re
import subprocess
from typing import Optional

import streamlit as st

PROJECT_ROOT = Path(__file__).resolve().parent.parent
INPUTS = PROJECT_ROOT / "tests" / "inputs"
OUTPUTS = PROJECT_ROOT / "tests" / "outputs"

# Token types from TokenType.java (same order as enum)
TOKEN_TYPES = [
    "EOF",
    "IDENT", "INT_LIT", "FLOAT_LIT", "STRING_LIT", "CHAR_LIT",
    "USE", "CONST", "TYPE", "RECORD", "AGENT", "WORLD",
    "INT", "FLOAT", "CHAR", "STRING", "BOOL", "VOID",
    "IF", "ELSE", "WHILE", "FOR", "REPEAT", "UNTIL", "RETURN", "BREAK", "CONTINUE",
    "READ", "PRINT",
    "TRUE", "FALSE",
    "MAIN",
    "SPAWN", "MOVE", "STEP", "NEIGHBORS", "RAND", "UPDATE", "DESTROY",
    "PRE", "POST",
    "SELF",
    "ASSERT",
    "ZONE", "RADIUS",
    "PLUS", "MINUS", "STAR", "SLASH", "MOD",
    "ASSIGN",
    "EQEQ", "NEQ", "LT", "LTE", "GT", "GTE",
    "ANDAND", "OROR", "NOT",
    "QMARK", "COLON",
    "SEMI", "COMMA", "DOT",
    "LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET",
]

SAMPLE_DESCRIPTIONS = {
    1: "Trader and Collector agents in a shared world; Collector drops items, Trader picks them up when within neighbor distance.",
    2: "Same traffic scenario (cars slow near intersections) shown two ways: using `neighbors()` in update vs using `zone`/radius. Two code variants in this section.",
    3: "Same traffic idea using `zone` and radius (standalone zone version).",
    4: "Agents move in a city and infect others when close; population can grow or shrink (e.g. death).",
    5: "Foragers move and consume energy; resource regrows in world post; temperature affects energy loss.",
}

LARGE_FILES = [
    ("LargeFile_Small.txt", "LargeFile_Small_BenchStats.txt"),
    ("LargeFile.txt", "LargeFile_BenchStats.txt"),
    ("LargeFile_Large.txt", "LargeFile_Large_BenchStats.txt"),
]


def parse_bench_stats(stats_path: Path) -> dict:
    text = stats_path.read_text()
    tokens = int(re.search(r"Tokens:\s*(\d+)", text).group(1))
    size = int(re.search(r"File size \(bytes\):\s*(\d+)", text).group(1))
    elapsed = None
    m = re.search(r"sample:\s*~(\d+)\s*ms", text)
    if m:
        elapsed = int(m.group(1))
    return {"tokens": tokens, "size": size, "elapsed_ms": elapsed}


def run_bench(input_path: Path) -> Optional[int]:
    """Run scanner --bench and return elapsed ms if successful."""
    cp = PROJECT_ROOT / "build" / "classes" / "java" / "main"
    if not cp.exists():
        return None
    try:
        out = subprocess.run(
            ["java", "-cp", str(cp), "src.Main", "--bench", str(input_path)],
            capture_output=True,
            text=True,
            timeout=120,
            cwd=PROJECT_ROOT,
        )
        if out.returncode != 0:
            return None
        m = re.search(r"Elapsed:\s*(\d+)\s*ms", out.stdout)
        return int(m.group(1)) if m else None
    except Exception:
        return None


st.set_page_config(page_title="CSC617M Scanner", layout="wide")
st.title("CSC617M Scanner")

# --- Token list ---
st.header("Token types")
st.caption("All token types produced by the scanner (from TokenType enum).")
st.text(", ".join(TOKEN_TYPES))

# --- Performance on large files ---
st.header("Performance on large files")
st.caption("Error-free large inputs only (no LargeFile_WithErrors).")

rows = []
for input_name, stats_name in LARGE_FILES:
    inp = INPUTS / input_name
    stats_path = OUTPUTS / stats_name
    if not inp.exists() or not stats_path.exists():
        continue
    data = parse_bench_stats(stats_path)
    size_kb = data["size"] / 1024
    size_str = f"{size_kb:.1f} KB" if size_kb < 1024 else f"{size_kb/1024:.2f} MB"
    elapsed = data.get("elapsed_ms")
    if elapsed is None:
        elapsed = run_bench(inp)
    elapsed_str = f"{elapsed} ms" if elapsed is not None else "—"
    rows.append({"File": input_name, "Size": size_str, "Tokens": data["tokens"], "Elapsed": elapsed_str})

if rows:
    st.table(rows)
else:
    st.info("No large-file stats found. Run the generator and ensure *\_BenchStats.txt exist in tests/outputs/.")

# --- Sample programs ---
st.header("Sample programs")

# For each sample: (num, title, [(input_file, output_file), ...], with_error_input, with_error_output)
samples = [
    (1, "Marketplace", [("Sample01_Marketplace.txt", "Sample01_Output.txt")], "Sample01_WithError.txt", "Sample01_WithError_Output.txt"),
    (2, "Traffic: Neighbors vs Zone", [("Sample02_TrafficNeighbors.txt", "Sample02_Output.txt"), ("Sample02_TrafficZone.txt", "Sample02_TrafficZone_Output.txt")], "Sample02_WithError.txt", "Sample02_WithError_Output.txt"),
    (3, "Traffic (Zone)", [("Sample03_TrafficZone.txt", "Sample03_Output.txt")], "Sample03_WithError.txt", "Sample03_WithError_Output.txt"),
    (4, "Disease spread", [("Sample04_DiseaseSpread.txt", "Sample04_Output.txt")], "Sample04_WithError.txt", "Sample04_WithError_Output.txt"),
    (5, "Resource competition", [("Sample05_ResourceCompetition.txt", "Sample05_Output.txt")], "Sample05_WithError.txt", "Sample05_WithError_Output.txt"),
]

for num, title, input_output_pairs, with_error_in, with_error_out in samples:
    with st.expander(f"Sample {num}: {title}", expanded=(num == 1)):
        st.markdown(f"**Description:** {SAMPLE_DESCRIPTIONS[num]}")
        for i, (iname, oname) in enumerate(input_output_pairs):
            if len(input_output_pairs) > 1:
                sub = "Using neighbors()" if i == 0 else "Using zone"
                st.subheader(sub)
            incode = INPUTS / iname
            outfile = OUTPUTS / oname
            if incode.exists():
                st.text_area("Code", incode.read_text(), height=200, key=f"code_{num}_{i}")
            else:
                st.warning(f"Missing {iname}")
            if outfile.exists():
                st.text_area("Scanner output (tokens)", outfile.read_text(), height=200, key=f"out_{num}_{i}")
            else:
                st.warning(f"Missing {oname}")
        # Same sample with intentional errors
        st.subheader("Version with errors")
        st.caption("Same program with a deliberate lexical error; scanner reports the error.")
        err_in = INPUTS / with_error_in
        err_out = OUTPUTS / with_error_out
        if err_in.exists():
            st.text_area("Code (with error)", err_in.read_text(), height=200, key=f"code_err_{num}")
        else:
            st.warning(f"Missing {with_error_in}")
        if err_out.exists():
            st.text_area("Scanner output (error)", err_out.read_text(), height=120, key=f"out_err_{num}")
        else:
            st.warning(f"Missing {with_error_out}")
