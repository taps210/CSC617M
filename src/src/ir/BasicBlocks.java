package src.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Partitions a linear list of IR instructions into basic blocks.
 * A basic block is a maximal sequence of instructions with a single entry (no branches into the middle)
 * and a single exit (only the last instruction may branch).
 */
public final class BasicBlocks {

    /**
     * One basic block: a list of instructions (may start with a label).
     */
    public record Block(int index, List<Instr> instructions) {
        public Block(int index, List<Instr> instructions) {
            this.index = index;
            this.instructions = instructions != null ? List.copyOf(instructions) : List.of();
        }

        public boolean startsWithLabel() {
            return !instructions.isEmpty() && instructions.get(0) instanceof Instr.LabelInstr;
        }

        public String label() {
            if (startsWithLabel()) {
                return ((Instr.LabelInstr) instructions.get(0)).label();
            }
            return "B" + index;
        }
    }

    /**
     * Partition instructions into basic blocks. Leaders are: (1) first instruction,
     * (2) every instruction that is a label, (3) every label that is the target of a branch.
     */
    public static List<Block> build(List<Instr> instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return List.of();
        }
        // Map label -> index of the LabelInstr (start of block for that label)
        var labelToIndex = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof Instr.LabelInstr lab) {
                labelToIndex.put(lab.label(), i);
            }
        }
        // Leaders: 0, every label index, every branch target
        Set<Integer> leaders = new TreeSet<>();
        leaders.add(0);
        for (int i = 0; i < instructions.size(); i++) {
            Instr in = instructions.get(i);
            if (in instanceof Instr.LabelInstr) {
                leaders.add(i);
            }
            if (in instanceof Instr.GotoInstr g) {
                Integer t = labelToIndex.get(g.label());
                if (t != null) leaders.add(t);
            }
            if (in instanceof Instr.IfGotoInstr ig) {
                Integer t = labelToIndex.get(ig.label());
                if (t != null) leaders.add(t);
            }
            if (in instanceof Instr.IfZeroGotoInstr iz) {
                Integer t = labelToIndex.get(iz.label());
                if (t != null) leaders.add(t);
            }
        }
        List<Integer> sorted = new ArrayList<>(leaders);
        List<Block> blocks = new ArrayList<>();
        for (int b = 0; b < sorted.size(); b++) {
            int start = sorted.get(b);
            int end = b + 1 < sorted.size() ? sorted.get(b + 1) : instructions.size();
            List<Instr> blockInstrs = new ArrayList<>();
            for (int i = start; i < end; i++) {
                blockInstrs.add(instructions.get(i));
            }
            blocks.add(new Block(b, blockInstrs));
        }
        return blocks;
    }
}
