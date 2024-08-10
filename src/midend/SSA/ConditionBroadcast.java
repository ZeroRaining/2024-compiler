package midend.SSA;

import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.Terminator;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;

/**
 * 整理所有块的跳转条件，从而让一个块知道进入自己所必须满足的条件
 */
public class ConditionBroadcast {
    public static void execute(ArrayList<Function> functions) {
        for (Function function : functions) {
            BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
            while (blk != null) {
                HashSet<BasicBlock> pres = blk.getPres();
                for (BasicBlock pre : pres) {
                    Instruction terminator = pre.getEndInstr();
                    if (terminator instanceof BranchInstr) {
                        blk.addCond(pre, ((BranchInstr) terminator).getCond());
                    }
                }
                blk = (BasicBlock) blk.getNext();
            }
        }
    }
    
    
}
