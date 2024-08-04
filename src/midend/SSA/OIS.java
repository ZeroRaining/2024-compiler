package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;

/**
 * Operation instruction simplification
 * 运算指令简化
 */
public class OIS {
    public static void execute(ArrayList<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        for (Function function : functions) {
            BasicBlock begin = (BasicBlock) function.getBasicBlocks().getHead();
            BasicBlock end   = (BasicBlock) function.getBasicBlocks().getTail();
            OSI4blks(begin, end);
        }
    }
    
    public static void OSI4blks(BasicBlock begin, BasicBlock end) {
        BasicBlock stop = (BasicBlock) end.getNext();
        BasicBlock basicBlock = begin;
        while (basicBlock != stop) {
            Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
            while (instruction != null) {
                Value simplified = instruction.operationSimplify();
                if (simplified != null) {
                    instruction.replaceUseTo(simplified);
                    instruction.removeFromList();
                }
                instruction = (Instruction) instruction.getNext();
            }
            basicBlock = (BasicBlock) basicBlock.getNext();
        }
    }
}
