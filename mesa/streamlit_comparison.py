"""
Herd (.hd) vs Mesa — ABM Language Comparison
Run: streamlit run mesa/streamlit_comparison.py
"""

import streamlit as st
from pathlib import Path

st.set_page_config(
    page_title="Herd vs Mesa — ABM Comparison",
    layout="wide",
)

ROOT = Path(__file__).parent.parent

def read_code(rel_path: str) -> str:
    return (ROOT / rel_path).read_text()

# ── captured outputs (deterministic snapshots) ───────────────────────────────

OUT_HD_01 = """\
=== Marketplace simulation starting ===
[World.pre] Spawned 3 Collectors and 2 Traders
  [Trader] picked up a drop ( 2 nearby)
  [Trader] picked up a drop ( 1 nearby)
  [Collector] dropping last item
  [Collector] dropping last item
  [Trader] picked up a drop ( 3 nearby)
  [Trader] picked up a drop ( 2 nearby)
  [Trader] picked up a drop ( 1 nearby)
  [Trader] picked up a drop ( 1 nearby)
  [Trader] picked up a drop ( 2 nearby)
  [Trader] picked up a drop ( 1 nearby)
[World.post] tick = 25
  [Trader] picked up a drop ( 3 nearby)
  [Trader] picked up a drop ( 2 nearby)
  [Trader] picked up a drop ( 2 nearby)
  [Trader] picked up a drop ( 1 nearby)
  [Trader] picked up a drop ( 1 nearby)
[World.post] tick = 50
[World.post] tick = 75
[World.post] tick = 100
=== Done. Ran 101 steps ==="""

OUT_MESA_01 = """\
=== Marketplace simulation starting ===
[World] Spawned 3 Collectors and 2 Traders
  [Collector] dropping last item
  [Collector] dropping last item
  [Collector] dropping last item
  [Trader] picked up a drop (4 nearby)
  [Trader] picked up a drop (4 nearby)
  [Trader] picked up a drop (4 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (5 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (5 nearby)
  [Trader] picked up a drop (5 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
[World.post] tick=25
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (2 nearby)
  [Trader] picked up a drop (1 nearby)
  [Trader] picked up a drop (1 nearby)
[World.post] tick=50
[World.post] tick=75
  [Trader] picked up a drop (1 nearby)
[World.post] tick=100
=== Done. Ran 100 steps ==="""

OUT_HD_02 = """\
=== Predator-Prey Simulation Starting ===
[World] 20 Deer, 4 Wolves on a 20x20 toroidal grid
  [x] Wolf kill (energy: 54 )
  [x] Wolf kill (energy: 54 )
  [x] Wolf kill (energy: 58 )
  [x] Wolf kill (energy: 53 )
  [x] Wolf kill (energy: 62 )
  [x] Wolf kill (energy: 57 )
  [x] Wolf kill (energy: 55 )
  [x] Wolf kill (energy: 60 )
  [x] Wolf kill (energy: 59 )
  [x] Wolf kill (energy: 64 )
  [x] Wolf kill (energy: 66 )
  [x] Wolf kill (energy: 70 )
  [x] Wolf kill (energy: 74 )
  [x] Wolf kill (energy: 57 )
  [x] Wolf kill (energy: 76 )
  [+] Wolf born
  [x] Wolf kill (energy: 40 )
  [x] Wolf kill (energy: 44 )
  [x] Wolf kill (energy: 48 )
  [x] Wolf kill (energy: 52 )
  [x] Wolf kill (energy: 56 )
  [x] Wolf kill (energy: 60 )
  [x] Wolf kill (energy: 52 )
  [x] Wolf kill (energy: 62 )
--- Tick 25 ---
  [x] Wolf kill (energy: 64 )
  [x] Wolf kill (energy: 51 )
  [x] Wolf kill (energy: 63 )
  [x] Wolf kill (energy: 67 )
  [x] Wolf kill (energy: 71 )
  [x] Wolf kill (energy: 75 )
  [x] Wolf kill (energy: 79 )
  [+] Wolf born
  [x] Wolf kill (energy: 43 )
  [x] Wolf kill (energy: 46 )
  [x] Wolf kill (energy: 49 )
--- Tick 50 ---
  [x] Wolf kill (energy: 54 )
  [x] Wolf kill (energy: 46 )
  [x] Wolf kill (energy: 62 )
  [x] Wolf kill (energy: 62 )
  [x] Wolf kill (energy: 46 )
--- Tick 75 ---
  [x] Wolf kill (energy: 49 )
  [x] Wolf kill (energy: 53 )
  [x] Wolf kill (energy: 55 )
  [x] Wolf kill (energy: 59 )
  [-] Wolf died (energy: 59 age: 81 )
  [-] Wolf died (energy: 59 age: 81 )
--- Tick 100 ---
--- Tick 125 ---
--- Tick 150 ---
--- Tick 175 ---
--- Tick 200 ---
=== Done. Ran 201 steps ==="""

OUT_MESA_02 = """\
=== Predator-Prey Simulation Starting ===
[World] 20 Deer, 4 Wolves on a 20x20 toroidal grid
  [x] Wolf kill (energy: 53)
  [x] Wolf kill (energy: 53)
--- Tick 25 ---
  [x] Wolf kill (energy: 22)
  [x] Wolf kill (energy: 25)
  [-] Wolf died (energy: 0 age: 50)
--- Tick 50 ---
  [x] Wolf kill (energy: 10)
  [x] Wolf kill (energy: 10)
  [x] Wolf kill (energy: 14)
  [x] Wolf kill (energy: 16)
  [x] Wolf kill (energy: 20)
--- Tick 75 ---
  [-] Wolf died (energy: 4 age: 81)
--- Tick 100 ---
--- Tick 125 ---
--- Tick 150 ---
--- Tick 175 ---
--- Tick 200 ---
=== Done. Ran 200 steps ==="""

OUT_HD_05 = """\
=== Zone Proximity + World Alarm Test ===
[World] Guard at (5,5) energy=50, Player at (0,0).
  [Guard] update energy = 45
[post] tick = 1 , alarmLevel = 0
  [Guard] update energy = 40
[post] tick = 2 , alarmLevel = 0
  [Guard] update energy = 35
[post] tick = 3 , alarmLevel = 1
  [Guard] update energy = 30
[post] tick = 4 , alarmLevel = 2
  [Guard] update energy = 25
  [zone nearPlayer] Guard energy low AND Player close! energy = 25 alarmLevel = 3
[post] tick = 5 , alarmLevel = 3
  [Guard] update energy = 20
  [zone nearPlayer] Guard energy low AND Player close! energy = 20 alarmLevel = 4
[post] tick = 6 , alarmLevel = 4
  [Guard] update energy = 15
  [zone nearPlayer] Guard energy low AND Player close! energy = 15 alarmLevel = 5
[post] tick = 7 , alarmLevel = 5
  [Guard] update energy = 10
  [zone nearPlayer] Guard energy low AND Player close! energy = 10 alarmLevel = 6
[post] tick = 8 , alarmLevel = 6
  [Guard] update energy = 5
  [zone nearPlayer] Guard energy low AND Player close! energy = 5 alarmLevel = 7
  [zone critical] Guard exhausted, dying. energy = 5 alarmLevel = 7
[post] tick = 9 , alarmLevel = 7
[post] tick = 10 , alarmLevel = 7
[post] tick = 11 , alarmLevel = 7
[post] tick = 12 , alarmLevel = 7
[post] tick = 13 , alarmLevel = 7
[post] tick = 14 , alarmLevel = 7
[post] tick = 15 , alarmLevel = 7
=== Done. Ran 15 steps ==="""

OUT_MESA_05 = """\
=== Zone Proximity + World Alarm Test ===
[World] Guard at (5,5) energy=50, Player at (0,0).
  [Guard] update energy=45
[post] tick=1, alarmLevel=0
  [Guard] update energy=40
[post] tick=2, alarmLevel=0
  [Guard] update energy=35
[post] tick=3, alarmLevel=1
  [Guard] update energy=30
[post] tick=4, alarmLevel=2
  [Guard] update energy=25
[post] tick=5, alarmLevel=3
  [zone nearPlayer] Guard energy low AND Player close! energy=25 alarmLevel=4
  [Guard] update energy=20
[post] tick=6, alarmLevel=4
  [zone nearPlayer] Guard energy low AND Player close! energy=20 alarmLevel=5
  [Guard] update energy=15
[post] tick=7, alarmLevel=5
  [zone nearPlayer] Guard energy low AND Player close! energy=15 alarmLevel=6
  [Guard] update energy=10
[post] tick=8, alarmLevel=6
  [zone nearPlayer] Guard energy low AND Player close! energy=10 alarmLevel=7
  [Guard] update energy=5
[post] tick=9, alarmLevel=7
  [zone nearPlayer] Guard energy low AND Player close! energy=5 alarmLevel=8
  [zone critical] Guard exhausted, dying. energy=5 alarmLevel=8
[post] tick=10, alarmLevel=8
[post] tick=11, alarmLevel=8
[post] tick=12, alarmLevel=8
[post] tick=13, alarmLevel=8
[post] tick=14, alarmLevel=8
[post] tick=15, alarmLevel=8
=== Done. Ran 15 steps ==="""

# ── page ─────────────────────────────────────────────────────────────────────

st.title("Herd vs Mesa — Agent-Based Modeling Language Comparison")
st.caption(
    "Side-by-side comparison of Herd (.hd), a custom domain-specific ABM language, "
    "against Mesa, the standard Python ABM framework."
)

# ── What is Mesa ─────────────────────────────────────────────────────────────

with st.expander("What is Mesa?", expanded=True):
    col1, col2 = st.columns([2, 1])
    with col1:
        st.markdown("""
**Mesa** is an open-source Python framework for **agent-based modeling (ABM)**,
developed at George Mason University and actively maintained by a community on GitHub.
It is the de-facto standard ABM library in the Python ecosystem.

#### What it is typically used for
- Social-science simulations (opinion spreading, market dynamics, segregation)
- Biological and ecological models (predator-prey, epidemics, flocking)
- Urban and traffic simulations
- Academic research where reproducibility and Python interoperability matter

#### Core concepts
| Concept | Mesa equivalent |
|---|---|
| Agent | `mesa.Agent` subclass with a `step()` method |
| World / Environment | `mesa.Model` subclass |
| Grid | `mesa.space.MultiGrid` or `SingleGrid` |
| Scheduling | `self.agents.shuffle_do("step")` |
| Data collection | `mesa.DataCollector` |
| Visualization | SolaraViz (optional, requires extra install) |

#### How a simulation runs
1. Create a `Model` — place agents on a grid in `__init__`
2. Call `model.step()` in a loop — it calls `shuffle_do("step")` on all agents
3. Each agent's `step()` reads/writes the model and grid directly
        """)
    with col2:
        st.info("""
**Quick facts**

- Language: Python 3
- Install: `pip install mesa`
- License: Apache 2.0
- Grid types: Single, Multi, Hex, Network, Continuous
- Scheduling: random activation, staged, time
- Viz: SolaraViz (browser), matplotlib
        """)

st.divider()

# ── Examples ──────────────────────────────────────────────────────────────────

tab1, tab2, tab3 = st.tabs([
    "Example 01 — Marketplace",
    "Example 02 — Predator-Prey",
    "Example 05 — Zone Proximity",
])


# ─── helper to render one comparison tab ─────────────────────────────────────
def render_tab(hd_path, mesa_path, hd_output, mesa_output,
               description, diff_rows, pros_hd, cons_hd, pros_mesa, cons_mesa):

    st.markdown(description)
    st.divider()

    # Code
    st.subheader("Code")
    c1, c2 = st.columns(2)
    with c1:
        st.markdown("**Herd (.hd)**")
        st.code(read_code(hd_path), language="java")
    with c2:
        st.markdown("**Mesa (Python)**")
        st.code(read_code(mesa_path), language="python")

    st.divider()

    # Output
    st.subheader("Output")
    c1, c2 = st.columns(2)
    with c1:
        st.markdown("**Herd output**")
        st.code(hd_output, language="text")
    with c2:
        st.markdown("**Mesa output**")
        st.code(mesa_output, language="text")

    st.divider()

    # Differences
    st.subheader("Key Differences")
    header = "| Feature | Herd (.hd) | Mesa (Python) |\n|---|---|---|\n"
    rows = "\n".join(f"| {r[0]} | {r[1]} | {r[2]} |" for r in diff_rows)
    st.markdown(header + rows)

    st.divider()

    # Pros / Cons
    st.subheader("Pros & Cons")
    c1, c2 = st.columns(2)
    with c1:
        st.markdown("**Herd (.hd)**")
        for p in pros_hd:
            st.success(f"+ {p}")
        for c in cons_hd:
            st.error(f"- {c}")
    with c2:
        st.markdown("**Mesa (Python)**")
        for p in pros_mesa:
            st.success(f"+ {p}")
        for c in cons_mesa:
            st.error(f"- {c}")


# ─── Tab 1: Marketplace ───────────────────────────────────────────────────────
with tab1:
    render_tab(
        hd_path="examples/Example01_Marketplace_Verbose.hd",
        mesa_path="mesa/mesa_Example01_Marketplace.py",
        hd_output=OUT_HD_01,
        mesa_output=OUT_MESA_01,
        description="""
**Scenario:** 3 `Collector` agents drop items as they wander. 2 `Trader` agents
pick up nearby `Drop` agents. Runs for 100 steps.
This example highlights the **agent lifecycle**, **spawning**, **typed neighbor arrays**,
and the `pre`/`post` world blocks.
        """,
        diff_rows=[
            ("Initialization",
             "`pre {}` block with `spawned` flag — runs before agents each step, guarded once",
             "All init in `__init__()` — no lifecycle separation"),
            ("Spawning an agent",
             "`spawn Drop(x, y)` — 1 built-in call",
             "Construct agent + call `grid.place_agent()` — 2 separate steps"),
            ("Typed neighbor arrays",
             "`Drop drops[]` — `neighbors()` auto-filters to `Drop` type",
             "Must use `isinstance(a, Drop)` list comprehension manually"),
            ("World post logic",
             "Dedicated `post {}` block — clearly separated from agent logic",
             "Manual code appended at end of `Model.step()`"),
            ("Built-in move",
             "`move(x, y)` — one call, grid-aware",
             "Must write `_move()` helper, manually clamp or wrap coordinates"),
            ("Step count",
             "`until (steps > 100)` → 101 steps",
             "`range(100)` → 100 steps (off by one vs .hd)"),
        ],
        pros_hd=[
            "`spawn`, `move`, `destroy` are first-class keywords",
            "Typed neighbor arrays eliminate boilerplate filtering",
            "`pre`/`post` blocks make lifecycle intent explicit",
            "World fields directly accessible (no `self.model.`)",
        ],
        cons_hd=[
            "Custom language — no Python ecosystem (numpy, pandas, etc.)",
            "No built-in visualization",
        ],
        pros_mesa=[
            "Full Python — easy to add matplotlib, pandas, etc.",
            "Active community, well-documented",
            "SolaraViz for interactive browser visualization",
        ],
        cons_mesa=[
            "Spawning is verbose — construct + place are two separate concerns",
            "No typed neighbors — `isinstance()` filtering is manual",
            "`self.model.field` boilerplate for shared world state",
            "`pre`/`post` semantics must be replicated by convention",
        ],
    )


# ─── Tab 2: Predator-Prey ────────────────────────────────────────────────────
with tab2:
    render_tab(
        hd_path="examples/Example02_PredatorPrey.hd",
        mesa_path="mesa/mesa_Example02_PredatorPrey.py",
        hd_output=OUT_HD_02,
        mesa_output=OUT_MESA_02,
        description="""
**Scenario:** 20 `Deer` wander a 20×20 toroidal grid and age out. 4 `Wolf` agents
hunt nearby agents, reproduce when energy is high, and die when energy runs out.
3 new deer are spawned every 4 ticks to sustain the ecosystem. Runs 200 steps.

This example highlights **toroidal wrapping**, **deferred spawn semantics**,
**typed neighbor arrays**, and **energy-based lifecycle**.
        """,
        diff_rows=[
            ("Typed neighbor array",
             "`Wolf nearby[]` — `neighbors(self, 2)` returns Wolf-typed agents nearby",
             "Must filter with `isinstance(a, Wolf)` — no implicit type constraint"),
            ("Toroidal move",
             "`move((x + dx + W) % W, ...)` — `move()` is built-in, wrapping is explicit math",
             "Manual `_move()` helper using `% grid.width`"),
            ("Deferred spawn",
             "`spawn Wolf(x,y)` takes effect next tick by language semantics",
             "Must implement `_pending` queue manually and flush after `shuffle_do`"),
            ("Deer replenishment",
             "`post {}` block — 3 deer every 4 ticks, clearly in world lifecycle",
             "Same logic in `step()` tail — no structural separation"),
            ("Step count",
             "`until (steps > 200)` → 201 steps",
             "`range(200)` → 200 steps"),
            ("Kill frequency",
             "~24+ kills in first 25 ticks — wolves hunt aggressively",
             "Fewer kills per run — wolf population is sparser"),
        ],
        pros_hd=[
            "Deferred spawn semantics are automatic — no queue to manage",
            "Typed neighbors eliminate manual isinstance filtering",
            "Toroidal world semantics are grid-agnostic (`move()` handles it)",
        ],
        cons_hd=[
            "No random seed control — output is non-deterministic",
        ],
        pros_mesa=[
            "Random seed can be fixed for reproducible runs",
            "Torus grid is a single `MultiGrid(..., torus=True)` parameter",
            "`_pending` pattern makes deferred spawn intent explicit",
        ],
        cons_mesa=[
            "Deferred spawn is not automatic — `_pending` queue must be hand-rolled",
            "No typed neighbors — type filtering is scattered across agent code",
            "Agents share no common move primitive — each agent writes its own `_move()`",
        ],
    )


# ─── Tab 3: Zone ─────────────────────────────────────────────────────────────
with tab3:
    render_tab(
        hd_path="examples/Example05_Zone.hd",
        mesa_path="mesa/mesa_Example05_Zone.py",
        hd_output=OUT_HD_05,
        mesa_output=OUT_MESA_05,
        description="""
**Scenario:** A `Guard` (energy 50, decreasing by 5 per tick) and a `Player` (moving
toward the Guard). When the Player enters radius 3, the world `alarmLevel` rises.
When it enters radius 5 and Guard energy < 10, the Guard dies.
Runs 15 steps.

This example highlights the **`zone` keyword** (proximity-triggered blocks) and
**execution order** differences between the two systems.
        """,
        diff_rows=[
            ("Proximity trigger",
             "`zone nearPlayer(3, Player) {}` — declared on the agent, fires automatically",
             "Two separate `get_neighbors()` calls inline in `step()` — no dedicated syntax"),
            ("Zone type filtering",
             "Zone radius + agent type declared in signature: `zone name(radius, AgentType)`",
             "`isinstance(a, Player)` filter in each neighbors call"),
            ("Shared world state",
             "`alarmLevel = alarmLevel + 1` — world fields directly accessible in zones",
             "`self.model.alarm_level += 1` — must go through model reference"),
            ("Execution order",
             "`update {}` runs FIRST, then zones fire (energy already decremented in zone body)",
             "Zones run BEFORE `update` — energy at zone-fire time is the PREVIOUS tick's value"),
            ("Final alarmLevel",
             "**7** — Guard dies on tick 9 (update decrements energy then zone fires)",
             "**8** — Guard dies on tick 10 (zone fires with stale energy, one extra fire)"),
            ("Multiple zones",
             "Multiple `zone` blocks declared cleanly on the agent",
             "Each zone is a separate `get_neighbors()` call — O(zones × ticks) queries"),
        ],
        pros_hd=[
            "`zone` keyword is declarative — proximity behavior is self-documenting",
            "Zone type and radius are part of the agent definition, not buried in `step()`",
            "Execution order (update → zones) is deterministic and language-defined",
            "World fields accessible without `self.model.` prefix",
        ],
        cons_hd=[
            "Zone execution order (after update) may be unintuitive",
            "No way to prioritize or conditionally skip zones",
            "Custom syntax — no IDE support or linting",
        ],
        pros_mesa=[
            "Zones are just Python — any condition can trigger them, not just proximity",
            "Full control over execution order within `step()`",
            "Easy to combine zone logic with other conditions (timers, flags, etc.)",
        ],
        cons_mesa=[
            "No `zone` primitive — each proximity trigger is a manual neighbors query",
            "Multiple zones = multiple `get_neighbors()` calls per tick (redundant work)",
            "Execution order must be manually maintained — easy to get wrong",
            "State access via `self.model.field` is verbose and error-prone",
        ],
    )

# ── Overall Summary ───────────────────────────────────────────────────────────
st.divider()
st.subheader("Overall Summary")

col1, col2 = st.columns(2)

with col1:
    st.markdown("#### When to use Herd (.hd)")
    st.markdown("""
- You want **concise, readable** ABM code focused on agent behavior
- Your model uses **typed neighbors**, **zones**, or heavy **spawn/destroy** cycles
- You are teaching or prototyping an ABM and want minimal boilerplate
- You need clear **pre/post world lifecycle** blocks
    """)

with col2:
    st.markdown("#### When to use Mesa")
    st.markdown("""
- You need the **Python ecosystem** (data analysis, visualization, ML integration)
- Reproducibility via **random seeds** is important
- You want **SolaraViz** or matplotlib for live visualization
- Your team is already Python-native and values ecosystem familiarity
- The model has **complex agent logic** that benefits from Python's full feature set
    """)

st.markdown("""
---
| Dimension | Herd (.hd) | Mesa |
|---|---|---|
| Verbosity | Low — built-ins for spawn/move/destroy/zone | High — everything is explicit Python |
| Typed neighbors | Yes — auto-filtered arrays | No — `isinstance()` filtering by hand |
| Zone / proximity triggers | `zone name(radius, Type) {}` keyword | Manual `get_neighbors()` per zone |
| World lifecycle | `pre {}` / `post {}` blocks | Conventional code in `__init__` / `step()` |
| Spawn semantics | Deferred (next-tick) by default | Manual `_pending` queue required |
| Ecosystem | None — standalone language | Full Python (numpy, pandas, matplotlib…) |
| Visualization | None built-in | SolaraViz, matplotlib |
| Execution order | update → zones (language-defined) | Manual, error-prone |
| Debugging | Custom IDE / Gradle | Python debugger, Jupyter, print |
| Random seed control | No | Yes (`seed=` on model) |
""")
