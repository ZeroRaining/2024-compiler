package midend.SSA;

import frontend.ir.DataType;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Function inlining
 * 函数内联，初步目标是将
 * todo: 值得注意的是，函数内联并不一定能提高运行效率，有时候一些不常用的代码（冷代码）可能作为函数调用存在会更合适一些
 */
public class FI {
    public static void doFI(ArrayList<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            while (basicBlock != null) {
                Instruction instr = (Instruction) basicBlock.getInstructions().getHead();
                while (instr != null) {
                    if (!(instr instanceof CallInstr) || ((CallInstr) instr).callsLibFunc()) {
                        instr = (Instruction) instr.getNext();
                        continue;
                    }
                    Function callee = (Function) (((CallInstr) instr).getFuncDef());
                    if (callee.isRecursive()) {
                        instr = (Instruction) instr.getNext();
                        continue;
                    }
                    // 将原本的基本块拆成两个
                    Instruction nextIns = (Instruction) instr.getNext();
                    int curDepth = basicBlock.getDepth();
                    BasicBlock nextBB = new BasicBlock(curDepth, function.getAndAddBlkIndex());
                    nextBB.insertAfter(basicBlock);
                    
                    while (nextIns != null) {
                        nextIns.removeFromList();
                        nextBB.addInstruction(nextIns);
                        nextIns = (Instruction) nextIns.getNext();
                    }
                    
                    ArrayList<BasicBlock> bbList = callee.func2blocks(curDepth);
                    
                    Collections.reverse(bbList);
                    for (BasicBlock newBB : bbList) {
                        newBB.insertAfter(basicBlock);
                    }
                    
                    JumpInstr prev2func = new JumpInstr(bbList.get(bbList.size() - 1));
                    prev2func.insertAfter(instr);
                    
                    BasicBlock funcLastBB = bbList.get(0);
                    ReturnInstr retIns = (ReturnInstr) funcLastBB.getEndInstr();
                    if (retIns.getDataType() != DataType.VOID) {
                        instr.replaceUseTo(retIns.getReturnValue());
                    }
                    retIns.removeFromList();
                    JumpInstr func2next = new JumpInstr(nextBB);
                    funcLastBB.addInstruction(func2next);
                    
                    instr.removeFromList();
                    instr = (Instruction) instr.getNext();
                }
                basicBlock = (BasicBlock) basicBlock.getNext();
            }
        }
    }
}
