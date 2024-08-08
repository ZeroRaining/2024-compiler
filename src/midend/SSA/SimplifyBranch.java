package midend.SSA;

import frontend.ir.Value;
import frontend.ir.constvalue.ConstBool;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import midend.loop.BlockType;

import java.util.ArrayList;

public class SimplifyBranch {
    private static boolean toBeContinue = false;
    public static void execute(ArrayList<Function> functions, boolean isAggressive) {
        toBeContinue = true;
        while (toBeContinue) {
            toBeContinue = false;
            for (Function function : functions) {
                Simplify(function, isAggressive);
            }
            toBeContinue = toBeContinue || RemoveUseLessPhi.execute(functions);
        }

    }
    //会出现两个块跳到同一个地方吗？//如果if内的语句为空？
    private static void Simplify(Function function, boolean isAggressive) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            if (blk.isBlockType(BlockType.NOTSIMPLE) && !isAggressive) {
                blk = (BasicBlock) blk.getNext();
                continue;
            }
            Instruction last = blk.getEndInstr();
            if (last instanceof BranchInstr) {
                Value cond = ((BranchInstr) last).getCond();
                if (cond instanceof ConstBool) {
                    last.removeFromList();
                    if (cond.getNumber().intValue() == 1) {
                        blk.setRet(false);
                        blk.addInstruction(new JumpInstr(((BranchInstr) last).getThenTarget()));
                    } else if (cond.getNumber().intValue() == 0) {
                        blk.setRet(false);
                        blk.addInstruction(new JumpInstr(((BranchInstr) last).getElseTarget()));
                    } else {
                        throw new RuntimeException("unexpected cond value");
                    }
                    toBeContinue = true;
                }
            }
            blk = (BasicBlock) blk.getNext();
        }
    }

}
