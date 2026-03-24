# Mesa equivalent of Example01_Marketplace_Verbose.hd
#
# Compare with: examples/Example01_Marketplace_Verbose.hd
#
# What this demonstrates vs the .hd version:
#   - No typed neighbor arrays — Trader must filter neighbors with isinstance(a, Drop)
#   - Spawning a Drop requires constructing it, registering with model, placing on grid
#     (3 concerns) vs a single spawn Drop(self.x, self.y) call
#   - No pre block — initialization lives in __init__, harder to guard
#   - No built-in move() — each agent needs its own _move() helper
#   - World post logic manually appended to Model.step()
#
# Run:
#   pip install mesa
#   python examples/mesa_Example01_Marketplace.py

import mesa


class Drop(mesa.Agent):
    """Passive agent representing a dropped item."""
    def step(self):
        pass  # Does nothing — equivalent to update {}


class Collector(mesa.Agent):
    def __init__(self, model):
        super().__init__(model)
        self.items = 5

    def step(self):
        self._move()
        if self.items == 1:
            print("  [Collector] dropping last item")
        if self.items > 0:
            # spawn Drop(self.x, self.y) in .hd is one line
            # Mesa requires: construct + place — two separate concerns
            drop = Drop(self.model)
            self.model.grid.place_agent(drop, self.pos)
            self.items -= 1

    def _move(self):
        x, y = self.pos
        dx = self.random.randint(-1, 1)
        dy = self.random.randint(-1, 1)
        new_x = max(0, min(19, x + dx))
        new_y = max(0, min(19, y + dy))
        self.model.grid.move_agent(self, (new_x, new_y))


class Trader(mesa.Agent):
    def step(self):
        self._move()
        # neighbors(self, 2) in .hd returns typed Drop[] directly
        # Mesa returns all agents — must filter manually with isinstance
        neighbors = self.model.grid.get_neighbors(
            self.pos, moore=True, radius=2, include_center=True
        )
        drops = [a for a in neighbors if isinstance(a, Drop)]
        if drops:
            print(f"  [Trader] picked up a drop ({len(drops)} nearby)")
            drops[0].remove()

    def _move(self):
        x, y = self.pos
        dx = self.random.randint(-1, 1)
        dy = self.random.randint(-1, 1)
        new_x = max(0, min(19, x + dx))
        new_y = max(0, min(19, y + dy))
        self.model.grid.move_agent(self, (new_x, new_y))


class Marketplace(mesa.Model):
    def __init__(self):
        super().__init__()
        self.grid = mesa.space.MultiGrid(20, 20, torus=False)
        self.tick = 0

        # No pre block — init happens here, no 'spawned' guard needed
        for _ in range(3):
            c = Collector(self)
            self.grid.place_agent(c, (self.random.randrange(20), self.random.randrange(20)))

        for _ in range(2):
            t = Trader(self)
            self.grid.place_agent(t, (self.random.randrange(20), self.random.randrange(20)))

        print("[World] Spawned 3 Collectors and 2 Traders")

    def step(self):
        self.agents.shuffle_do("step")

        # post logic — manually placed here, not a separate named block
        self.tick += 1
        if self.tick % 25 == 0:
            print(f"[World.post] tick={self.tick}")


if __name__ == "__main__":
    steps = 0
    print("=== Marketplace simulation starting ===")
    model = Marketplace()
    for _ in range(100):
        model.step()
        steps += 1
    print(f"=== Done. Ran {steps} steps ===")
