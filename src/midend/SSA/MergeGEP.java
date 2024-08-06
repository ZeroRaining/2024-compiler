package midend.SSA;

import backend.itemStructure.Group;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.binop.AddInstr;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;

public class MergeGEP {
    public static void execute(ArrayList<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            while (basicBlock != null) {
                Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
                while (instruction != null) {
                    if (instruction instanceof GEPInstr) {
                        Group<AddInstr, GEPInstr> mergedGroup = ((GEPInstr) instruction).tryMergePtr(function);
                        if (mergedGroup != null) {
                            AddInstr link = mergedGroup.getFirst();
                            GEPInstr merged = mergedGroup.getSecond();
                            link.insertBefore(instruction);
                            merged.insertBefore(instruction);
                            instruction.replaceUseTo(merged);
                            instruction.removeFromList();
                        }
                    }
                    instruction = (Instruction) instruction.getNext();
                }
                basicBlock = (BasicBlock) basicBlock.getNext();
            }
        }
    }
}
