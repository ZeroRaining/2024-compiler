package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.HashSet;

public class MergeBlock {
    public static void execute(HashSet<Function> functions) {
        for (Function function : functions) {
            merge(function);
        }
    }

    private static void merge(Function function) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        //删除不要的块
        while (blk != null) {
            Instruction last = blk.getEndInstr();
            if (last instanceof JumpInstr) {
                Instruction tmp = last;
                Instruction next = ((JumpInstr) tmp).getTarget().getEndInstr();
                //tmp是最后一个只有一个jumpInstr的块的jump语句
                //next为tmp的下一个块的最后一条语句，有可能为return
                while (next instanceof JumpInstr
                        && next.getParentBB().getInstructions().getSize() == 1
                        && next.getParentBB().getUserSet().size() == 1) {
                    tmp = next;
                    next = ((JumpInstr) next).getTarget().getEndInstr();
                }
                if (tmp != last) {
                    last.modifyUse(((JumpInstr) last).getTarget(), ((JumpInstr) tmp).getTarget());
                }
                if (next instanceof ReturnInstr && last.getParentBB().getInstructions().getSize() == 1) {
                    Value to = next.getParentBB();
                    Value from = last.getParentBB();
                    from.replaceUseTo(to);
                    last.getParentBB().removeFromList();
                }
            }
            blk = (BasicBlock) blk.getNext();
        }

    }
}
