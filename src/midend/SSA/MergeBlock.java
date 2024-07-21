package midend.SSA;

import Utils.CustomList;
import arg.Arg;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class MergeBlock {
    public static void execute(HashSet<Function> functions) {
        for (Function function : functions) {
            merge(function);
        }
        RemoveUseLessPhi.execute(functions);
    }

    private static boolean check4merge(Instruction instr) {
        if (instr.getParentBB().getSucs().size() != 1) {
            return false;
        }
        assert instr instanceof JumpInstr;
        if (((JumpInstr) instr).getTarget().getPres().size() != 1) {
            return false;
        }
        return true;
    }
    private static void merge(Function function) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        //删除不要的块
        while (blk != null) {
            Instruction last = blk.getEndInstr();
            CustomList ins = new CustomList();
            ins.addToTail(last);
            if (check4merge(last)) {
                //效率太慢
                BasicBlock nextBlk = ((JumpInstr) last).getTarget();
                last = (Instruction) last.getPrev();
                while (last != null) {
                    Instruction tmp = (Instruction) last.getPrev();
                    last.setParentBB(nextBlk);
                    nextBlk.getInstructions().addToHead(last);
                    last = tmp;
                }
                Instruction phi = (Instruction) nextBlk.getInstructions().getHead();
                while (phi instanceof PhiInstr) {
                    ArrayList<BasicBlock> prts = ((PhiInstr) phi).getPrtBlks();
                    assert prts.size() == 1;
                    phi.replaceUseTo(((PhiInstr) phi).getValues().get(0));
                    phi.removeFromList();
                    phi = (Instruction) phi.getNext();
                }
                blk.setInstructions(ins);
                blk.replaceUseTo(nextBlk);
//                blk.removeFromList();
            }
            blk = (BasicBlock) blk.getNext();
        }

    }



    private static boolean check4remove(Instruction next) {
        if (next.getParentBB().getInstructions().getSize() != 1) {
            return false;
        }
        if (next.getParentBB().getPres().size() != 1) {
            return false;
        }
        return true;
    }
}
