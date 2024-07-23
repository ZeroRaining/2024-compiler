package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashSet;

public class RemoveUseLessPhi {
    public static boolean execute(HashSet<Function> functions) {
        boolean toBeContinue = false;
        for (Function function : functions) {
            toBeContinue = removeUnused(function) | toBeContinue;
        }
        return toBeContinue;
    }

    private static boolean removeUnused(Function function) {
        //phi的block的setUse应该是要加的
        boolean toBeContinue = false;
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            Instruction instr = (Instruction) blk.getInstructions().getHead();
            while (instr instanceof PhiInstr) {
                ArrayList<BasicBlock> prts = ((PhiInstr) instr).getPrtBlks();
                for (int i = 0; i < prts.size(); i++) {
                    if (!blk.getPres().contains(prts.get(i))) {
                        prts.remove(i);
                        ((PhiInstr) instr).getValues().remove(i);
                        i--;
                        toBeContinue = true;
                    }
                }
                if (prts.isEmpty()) {
                    instr.removeFromList();
                    toBeContinue = true;
                } else if (((PhiInstr) instr).canSimplify()) {
                    Value value = ((PhiInstr) instr).getValues().get(0);
                    instr.replaceUseTo(value);
                    instr.removeFromList();
                    toBeContinue = true;
                }
                instr = (Instruction) instr.getNext();
            }
            blk = (BasicBlock) blk.getNext();
        }
        return toBeContinue;
    }
}