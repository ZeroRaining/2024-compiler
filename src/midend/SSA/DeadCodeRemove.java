package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.binop.BinaryOperation;
import frontend.ir.instr.convop.ConversionOperation;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.MemoryOperation;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.otherop.cmp.Cmp;
import frontend.ir.instr.unaryop.FNegInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class DeadCodeRemove {
    public static void doDeadCodeRemove(HashSet<Function> functions) {
        for (Function function : functions) {
            removeCode(function);
        }
    }

    private static void removeCode(Function function) {
        //获得从未被使用的变量
        Queue<Instruction> noUse = new LinkedList<>();
        BasicBlock block = (BasicBlock) function.getBasicBlocks().getHead();
        while (block != null) {
            Instruction instr = (Instruction) block.getInstructions().getHead();
            while (instr != null) {
                if (instr.getBeginUse() == null && checkNoSideEffect(instr)) {
                    noUse.add(instr);
                }
                instr = (Instruction) instr.getNext();
            }
            block = (BasicBlock) block.getNext();
        }

        while (!noUse.isEmpty()) {
            Instruction instr = noUse.poll();
            for (Value value : instr.getUseValueList()) {
                if (checkNoSideEffect(value) && value.getBeginUse() == null) {
                    assert value instanceof Instruction;
                    noUse.add((Instruction) value);
                }
            }
            instr.removeFromList();
        }

    }

    private static boolean checkNoSideEffect(Value value) {
        if (value instanceof PhiInstr) {
            return true;
        } else if (value instanceof BinaryOperation) {
            return true;
        } else if (value instanceof ConversionOperation) {
            return true;
        } else if (value instanceof MemoryOperation && !(value instanceof StoreInstr)){
            return true;
        } else if (value instanceof Cmp) {
            return true;
        } else if (value instanceof FNegInstr) {
            return true;
        }
        return false;
        // store i32 1 i32* p
    }
}
