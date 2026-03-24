# Mesa equivalent of Example02_PredatorPrey.hd
#
# Compare with: examples/Example02_PredatorPrey.hd
#
# What this demonstrates vs the .hd version:
#   - Agents need a separate _move() helper because move is not a built-in
#   - Removing an agent requires TWO calls: grid.remove_agent + model.agents
#     (handled by self.remove() in Mesa 2.x, but still must be remembered)
#   - Neighbor filtering uses isinstance() — not typed like Drop drops[]
#   - World "post" logic must be manually placed at end of Model.step()
#   - Spawning a new agent requires placing it in grid AND registering it
#
# Run:
#   pip install mesa
#   python examples/mesa_Example02_PredatorPrey.py

import mesa


class Deer(mesa.Agent):
    def __init__(self, model):
        super().__init__(model)
        self.age = 0

    def step(self):
        self._move(1)
        self.age += 1
        if self.age > 30:
            self.remove()

    def _move(self, speed):
        # No built-in move() — must manually compute newis position and call grid
        x, y = self.pos
        dx = self.random.randint(-speed, speed)
        dy = self.random.randint(-speed, speed)
        new_x = (x + dx) % self.model.grid.width
        new_y = (y + dy) % self.model.grid.height
        self.model.grid.move_agent(self, (new_x, new_y))


class Wolf(mesa.Agent):
    def __init__(self, model):
        super().__init__(model)
        self.energy = 50
        self.age = 0

    def step(self):
        self._move(2)
        self.energy -= 1
        self.age += 1

        # .hd declares: Wolf nearby[] — neighbors() returns only Wolf-typed agents
        # wolves hunt other wolves (intra-species), not Deer
        neighbors = self.model.grid.get_neighbors(
            self.pos, moore=True, radius=2, include_center=False
        )
        nearby = [a for a in neighbors if isinstance(a, Wolf)]
        if nearby:
            nearby[0].remove()
            self.energy += 5
            print(f"  [x] Wolf kill (energy: {self.energy})")

        if self.energy > 75:
            # Queue cub for next tick — matches .hd spawn semantics
            self.model.spawn(Wolf, self.pos)
            self.energy -= 40
            print("  [+] Wolf born")

        if self.energy <= 0 or self.age > 80:
            print(f"  [-] Wolf died (energy: {self.energy} age: {self.age})")
            self.remove()

    def _move(self, speed):
        x, y = self.pos
        dx = self.random.randint(-speed, speed)
        dy = self.random.randint(-speed, speed)
        new_x = (x + dx) % self.model.grid.width
        new_y = (y + dy) % self.model.grid.height
        self.model.grid.move_agent(self, (new_x, new_y))


class Plain(mesa.Model):
    def __init__(self):
        super().__init__()
        # Grid setup is explicit — in .hd, world width/height is just two lines
        self.grid = mesa.space.MultiGrid(20, 20, torus=True)
        self.tick = 0
        # Pending agents are queued here and added AFTER shuffle_do finishes,
        # matching .hd behavior where spawn takes effect next tick
        self._pending = []

        # No pre block — initialization is done manually in __init__
        for _ in range(20):
            deer = Deer(self)
            x = self.random.randrange(20)
            y = self.random.randrange(20)
            self.grid.place_agent(deer, (x, y))

        for _ in range(4):
            wolf = Wolf(self)
            x = self.random.randrange(20)
            y = self.random.randrange(20)
            self.grid.place_agent(wolf, (x, y))

        print("[World] 20 Deer, 4 Wolves on a 20x20 toroidal grid")

    def spawn(self, agent_class, pos):
        """Queue an agent for next tick — mirrors .hd spawn behavior."""
        self._pending.append((agent_class, pos))

    def step(self):
        # Agent updates
        self.agents.shuffle_do("step")

        # Flush pending spawns AFTER all agents have stepped (next-tick semantics)
        for agent_class, pos in self._pending:
            a = agent_class(self)
            self.grid.place_agent(a, pos)
        self._pending.clear()

        # No post block — world post logic must be manually placed here
        self.tick += 1
        if self.tick % 4 == 0:
            for _ in range(3):
                x = self.random.randrange(20)
                y = self.random.randrange(20)
                self.spawn(Deer, (x, y))
        if self.tick % 25 == 0:
            print(f"--- Tick {self.tick} ---")


if __name__ == "__main__":
    print("=== Predator-Prey Simulation Starting ===")
    model = Plain()
    for _ in range(200):
        model.step()
    print("=== Done. Ran 200 steps ===")
