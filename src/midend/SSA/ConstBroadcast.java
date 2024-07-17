package midend.SSA;

import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class ConstBroadcast {
    public static void doConstBroadcast(HashSet<Function> functions) {
        for (Function function : functions) {
            boardcast(function);
        }
    }

    private static void boardcast(Function function) {
        Queue<Instruction> allIns = function.getAllInstr();
        while (!allIns.isEmpty()) {
            Instruction instr = allIns.poll();
            if (instr instanceof PhiInstr) {
//                ((PhiInstr) instr).canSimplify()
            }
        }

    }
}
