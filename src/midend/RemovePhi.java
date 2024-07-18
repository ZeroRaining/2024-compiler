package midend;

import debug.DEBUG;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.Move;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.HashSet;

public class RemovePhi {
    private static int blkCnt = 0;

    public static void removePhi(HashSet<Function> functions) {
        for (Function function : functions) {
            remove(function);
        }
    }

    private static void remove(Function function) {
        blkCnt = ((BasicBlock)function.getBasicBlocks().getTail()).getLabelCnt();
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            if (!(blk.getInstructions().getHead() instanceof PhiInstr)) {
                blk = (BasicBlock) blk.getNext();
                continue;
            }
            Instruction instr = (Instruction) blk.getInstructions().getHead();
            while (instr instanceof PhiInstr) {
                PhiInstr phi = (PhiInstr) instr;
                for (int i = 0; i < phi.getPrtBlks().size(); i++) {
                    BasicBlock pre = phi.getPrtBlks().get(i);
                    Value src = phi.getValues().get(i);

                    if (pre.getSucs().size() == 1) {
                        Move move = new Move(src, phi);
                        move.insertBefore(pre.getEndInstr());
                    } else if (pre.getSucs().size() == 2){
                        BranchInstr branch = (BranchInstr) pre.getEndInstr();
                        BasicBlock newBlk = new BasicBlock(pre.getDepth());
                        newBlk.insertAfter(pre);
                        branch.modifyUse(blk, newBlk);
                        newBlk.setLabelCnt(blkCnt++);
                        newBlk.addInstruction(new JumpInstr(blk));

                        //TODO：对于前驱后继的修改，是否应该放在新建指令时？
                        pre.getSucs().remove(blk);
                        pre.getSucs().add(newBlk);
                        newBlk.getSucs().add(blk);
                        newBlk.getPres().add(pre);
                        blk.getPres().remove(pre);
                        blk.getPres().add(newBlk);
                    } else if (pre.getSucs().size() != 0){
                        DEBUG.dbgPrint(pre.value2string());
                        DEBUG.dbgPrint2("sucs size: " + pre.getSucs());
                        throw new RuntimeException("too many sucs");
                    }
                }
                phi.removeFromList();
                instr = (Instruction) instr.getNext();
            }
        }
    }
}
