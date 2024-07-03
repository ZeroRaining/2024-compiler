package midend.SSA;

import Utils.CustomList;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.*;

public class DFG {
    private Queue<Integer> worklist;
    private HashMap<Integer, Boolean> visited;
    private HashMap<Integer, Boolean> placed;
    private ArrayList<Function> functions;
    public DFG(ArrayList<Function> functions) {
        worklist = new LinkedList<Integer>();
        visited = new HashMap<>();
        placed = new HashMap<>();
        this.functions = functions;
        for (Function function : functions) {
            removeBlk(function);
        }
    }

    private void removeBlk(Function function) {
        Iterator<CustomList.Node> blks = function.getBasicBlocks().iterator();
        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            Instruction instr = (Instruction) block.getInstructions().getTail();
            if (instr instanceof JumpInstr) {

            }
        }
    }


}
