# Mesa equivalent of Example05_Zone.hd
#
# Compare with: examples/Example05_Zone.hd
#
# What this demonstrates vs the .hd version:
#   - Mesa has NO zone concept — proximity triggers must be written manually
#     inside step() using if/elif chains with get_neighbors() calls
#   - Each zone threshold needs its own neighbors query (two separate calls)
#   - The zone conditions are scattered inline, not declared separately on the agent
#   - World shared state (alarm_level) must be accessed via self.model explicitly
#   - There is no way to declare "fire this block when X is within radius R"
#     without embedding that logic into the step function manually
#
# Run:
#   pip install mesa
#   python examples/mesa_Example05_Zone.py

import math
import mesa


class Player(mesa.Agent):
    def step(self):
        # Move +1 toward (5, 5)
        x, y = self.pos
        nx = x + 1 if x < 5 else x
        ny = y + 1 if y < 5 else y
        self.model.grid.move_agent(self, (nx, ny))


class Guard(mesa.Agent):
    def __init__(self, model):
        super().__init__(model)
        self.energy = 50

    def step(self):
        # ---- zone nearPlayer(3, Player) ----
        # No zone keyword — must manually query neighbors and check type
        near3 = self.model.grid.get_neighbors(
            self.pos, moore=True, radius=3, include_center=True
        )
        players_near3 = [a for a in near3 if isinstance(a, Player)]
        if players_near3:
            self.model.alarm_level += 1
            if self.energy < 30:
                print(
                    f"  [zone nearPlayer] Guard energy low AND Player close!"
                    f" energy={self.energy} alarmLevel={self.model.alarm_level}"
                )

        # ---- zone critical(5, Player) ----
        # Second separate neighbors query for a different radius
        near5 = self.model.grid.get_neighbors(
            self.pos, moore=True, radius=5, include_center=True
        )
        players_near5 = [a for a in near5 if isinstance(a, Player)]
        if players_near5 and self.energy < 10:
            print(
                f"  [zone critical] Guard exhausted, dying."
                f" energy={self.energy} alarmLevel={self.model.alarm_level}"
            )
            self.remove()
            return

        # ---- update block ----
        self.energy -= 5
        print(f"  [Guard] update energy={self.energy}")


class Arena(mesa.Model):
    def __init__(self):
        super().__init__()
        self.grid = mesa.space.MultiGrid(10, 10, torus=False)
        self.tick = 0
        self.alarm_level = 0  # shared world state — accessed via self.model in agents

        guard = Guard(self)
        self.grid.place_agent(guard, (5, 5))

        player = Player(self)
        self.grid.place_agent(player, (0, 0))

        print("[World] Guard at (5,5) energy=50, Player at (0,0).")

    def step(self):
        self.agents.shuffle_do("step")
        self.tick += 1
        print(f"[post] tick={self.tick}, alarmLevel={self.alarm_level}")


if __name__ == "__main__":
    print("=== Zone Proximity + World Alarm Test ===")
    model = Arena()
    for _ in range(15):
        model.step()
    print("=== Done. Ran 15 steps ===")
