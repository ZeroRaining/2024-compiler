package midend.SSA;

import Utils.CustomList;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;

public class MergeBlock {
    public static void execute(ArrayList<Function> functions, boolean isAgressive) {
        for (Function function : functions) {
            merge(function, isAgressive);
        }
        RemoveUseLessPhi.execute(functions);
    }

    private static boolean check4merge(Instruction instr) {
        if (instr.getParentBB().getSucs().size() != 1) {
            return false;
        }
        if (!(instr instanceof JumpInstr)) {
            throw new RuntimeException(instr.print() + "\nParentBB: " + instr.getParentBB() + "\nSucs: " + instr.getParentBB().getSucs());
        }
        if (((JumpInstr) instr).getTarget().getPres().size() != 1) {
            return false;
        }
        return true;
    }

    private static void merge(Function function, boolean isAgressive) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead().getNext();
        //合并不要的块
        while (blk != null) {
            Instruction last = blk.getEndInstr();

            if (check4merge(last)) {
                if (!isAgressive && check4entering(last)) {
                    blk = (BasicBlock) blk.getNext();
                    continue;
                }
                last.removeFromList();
                //效率太慢
                BasicBlock nextBlk = ((JumpInstr) last).getTarget();
                Instruction phi = (Instruction) nextBlk.getInstructions().getHead();
                while (phi instanceof PhiInstr) {
                    ArrayList<BasicBlock> prts = ((PhiInstr) phi).getPrtBlks();
                    assert prts.size() == 1;
                    phi.replaceUseTo(((PhiInstr) phi).getValues().get(0));
                    phi.removeFromList();
                    phi = (Instruction) phi.getNext();
                }
                last = (Instruction) last.getPrev();
                if (last != null) {
                    //link last <-> nextblk.fisrtInstr
                    last.linked(nextBlk.getInstructions().getHead());
                    CustomList ins = new CustomList();
                    blk.setInstructions(ins);
                }
                blk.replaceUseTo(nextBlk);
                blk.removeFromList();
            }
            blk = (BasicBlock) blk.getNext();
        }

    }



    private static boolean check4entering(Instruction instr) {
        return instr.getParentBB().isEntering() || ((JumpInstr) instr).getTarget().isEntering();
    }
}
