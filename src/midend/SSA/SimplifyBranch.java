package midend.SSA;

import frontend.ir.Value;
import frontend.ir.constvalue.ConstBool;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class SimplifyBranch {
    private static boolean toBeContinue = false;
    public static void execute(HashSet<Function> functions) {
        for (Function function : functions) {
            toBeContinue = true;
            while (toBeContinue) {
                toBeContinue = false;
                Simplify(function);
                removeUselessPhi(function);
            }
        }
    }

    private static void removeUselessPhi(Function function) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            Instruction instr = (Instruction) blk.getInstructions().getHead();
            while (instr instanceof PhiInstr) {
                ArrayList<BasicBlock> prts = ((PhiInstr) instr).getPrtBlks();
                for (int i = 0; i < prts.size(); i++) {
                    if (prts.get(i).getBeginUse() == null) {
                        prts.remove(i);
                        ((PhiInstr) instr).getValues().remove(i);
                        i--;
                        toBeContinue = true;
                    }
                }
                if (prts.isEmpty()) {
                    instr.removeFromList();
                    toBeContinue = true;
                } else if (prts.size() == 1) {
                    Value value = ((PhiInstr) instr).getValues().get(0);
                    instr.replaceUseTo(value);
                    instr.removeFromList();
                    toBeContinue = true;
                }
                instr = (Instruction) instr.getNext();
            }
            blk = (BasicBlock) blk.getNext();
        }
    }

    private static void Simplify(Function function) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            Instruction last = blk.getEndInstr();
            if (last instanceof BranchInstr) {
                Value cond = ((BranchInstr) last).getCond();
                if (cond instanceof ConstBool) {
                    if (cond.getNumber().intValue() == 1) {
                        blk.addInstruction(new JumpInstr(((BranchInstr) last).getThenTarget()));
                    } else if (cond.getNumber().intValue() == 0) {
                        blk.addInstruction(new JumpInstr(((BranchInstr) last).getElseTarget()));
                    } else {
                        throw new RuntimeException("unexpected cond value");
                    }
                    last.removeFromList();
                    toBeContinue = true;
                }
            }
            blk = (BasicBlock) blk.getNext();
        }
    }


}
