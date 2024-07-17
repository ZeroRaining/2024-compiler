package midend.SSA;

import frontend.ir.Use;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.HashMap;
import java.util.HashSet;

public class GVN {
    public static void doGVN(HashSet<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        for (Function function : functions) {
            HashMap<String, Instruction> instructions = new HashMap<>();
            
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            while (basicBlock != null) {
                Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
                while (instruction != null) {
                    if (instructions.containsKey(instruction.myHash())) {
                        Instruction basicInstr = instructions.get(instruction.myHash());
                        instruction.replaceUseTo(basicInstr);
                        instruction.removeFromList();
                    } else {
                        instructions.put(instruction.myHash(), instruction);
                    }
                    instruction = (Instruction) instruction.getNext();
                }
                basicBlock = (BasicBlock) basicBlock.getNext();
            }
        }
    }
}
