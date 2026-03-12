package src.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Control flow graph: nodes are basic blocks, edges are successor relationships.
 */
public final class ControlFlowGraph {

    private final List<BasicBlocks.Block> blocks;
    /** label -> block index */
    private final Map<String, Integer> labelToBlockIndex;
    /** successors.get(i) = set of block indices that block i can transfer to */
    private final List<Set<Integer>> successors;

    public ControlFlowGraph(List<BasicBlocks.Block> blocks) {
        this.blocks = blocks != null ? List.copyOf(blocks) : List.of();
        this.labelToBlockIndex = new HashMap<>();
        for (int i = 0; i < this.blocks.size(); i++) {
            BasicBlocks.Block b = this.blocks.get(i);
            if (b.startsWithLabel()) {
                String lab = ((Instr.LabelInstr) b.instructions().get(0)).label();
                labelToBlockIndex.put(lab, i);
            }
        }
        this.successors = new ArrayList<>();
        for (int i = 0; i < this.blocks.size(); i++) {
            successors.add(new HashSet<>());
        }
        for (int i = 0; i < this.blocks.size(); i++) {
            BasicBlocks.Block b = this.blocks.get(i);
            if (b.instructions().isEmpty()) continue;
            Instr last = b.instructions().get(b.instructions().size() - 1);
            if (last instanceof Instr.GotoInstr g) {
                Integer target = labelToBlockIndex.get(g.label());
                if (target != null) successors.get(i).add(target);
            } else if (last instanceof Instr.IfGotoInstr ig) {
                Integer target = labelToBlockIndex.get(ig.label());
                if (target != null) successors.get(i).add(target);
                if (i + 1 < this.blocks.size()) successors.get(i).add(i + 1);
            } else if (last instanceof Instr.IfZeroGotoInstr iz) {
                Integer target = labelToBlockIndex.get(iz.label());
                if (target != null) successors.get(i).add(target);
                if (i + 1 < this.blocks.size()) successors.get(i).add(i + 1);
            } else if (last instanceof Instr.ReturnInstr) {
                // no successors
            } else {
                if (i + 1 < this.blocks.size()) successors.get(i).add(i + 1);
            }
        }
    }

    public List<BasicBlocks.Block> blocks() {
        return blocks;
    }

    public Set<Integer> successors(int blockIndex) {
        if (blockIndex < 0 || blockIndex >= successors.size()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(successors.get(blockIndex));
    }

    public int blockCount() {
        return blocks.size();
    }
}
