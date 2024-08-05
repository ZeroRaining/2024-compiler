package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import frontend.ir.structure.GlobalObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 针对指针的 mem2reg，可以认为是在做别名分析
 * 现在的想法是按照控制树 dfs，保存对于一个地址的赋值记录，如果 dom 分叉（直接控制块不止一个）则清空当前列表，以避免多重定义导致冲突。
 * 对于 call 需要特殊处理，函数中可能出现修改指针指向内存内容的操作。
 * 必须在 GVN 之后，因为需要判断是不是同一个指针
 */
public class PtrMem2Reg {
    private static HashSet<GEPInstr> gepUsed;
    private static HashSet<Value> rootMightUsed;
    private static boolean DONT_MOVE;
    
    public static void execute(ArrayList<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        
        gepUsed = new HashSet<>();
        rootMightUsed = new HashSet<>();
        DONT_MOVE = false;
        
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            dfsIdoms(basicBlock, new HashMap<>());
        }
        
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            while (basicBlock != null) {
                Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
                while (instruction != null) {
                    if (instruction instanceof StoreInstr) {
                        Value ptr = ((StoreInstr) instruction).getPtr();
                        if (ptr instanceof GEPInstr) {
                            if (!DONT_MOVE && !gepUsed.contains((GEPInstr) ptr) &&
                                    !rootMightUsed.contains(((GEPInstr) ptr).getRoot())) {
                                instruction.removeFromList();
                            }
                        } else if (!(ptr instanceof GlobalObject)) {
                            throw new RuntimeException("这里 load 和 store 的指针应该只能是 gep 或者全局对象");
                        }
                    }
                    instruction = (Instruction) instruction.getNext();
                }
                basicBlock = (BasicBlock) basicBlock.getNext();
            }
        }
    }
    
    private static void dfsIdoms(BasicBlock basicBlock, HashMap<GEPInstr, Value> defMap) {
        if (basicBlock == null) {
            throw new NullPointerException();
        }
        
        Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
        while (instruction != null) {
            if (instruction instanceof CallInstr) {
                // todo: 遇到函数调用直接摆烂，之后要确认一下是不是真的跟指针有关系。
                defMap.clear();
                DONT_MOVE = true;
            } else if (instruction instanceof StoreInstr) {
                Value ptr = ((StoreInstr) instruction).getPtr();
                if (ptr instanceof GEPInstr) {
                    // root 只能是 AllocaInstr，GlobalObject，FParam 三种
                    Value root = ((GEPInstr) ptr).getRoot();
                    if (((GEPInstr) ptr).hasNonConstIndex()) {
                        defMap.keySet().removeIf(oldPtr -> oldPtr.getRoot().equals(root));
                    } else {
                        defMap.put((GEPInstr) ptr, ((StoreInstr) instruction).getValue());
                    }
                } else if (!(ptr instanceof GlobalObject)) {
                    throw new RuntimeException("这里 load 和 store 的指针应该只能是 gep 或者全局对象");
                }
            } else if (instruction instanceof LoadInstr) {
                Value ptr = ((LoadInstr) instruction).getPtr();
                if (ptr instanceof GEPInstr) {
                    if (!((GEPInstr) ptr).hasNonConstIndex()) {
                        Value defVal = defMap.get((GEPInstr) ptr);
                        if (defVal != null) {
                            instruction.replaceUseTo(defVal);
                            instruction.removeFromList();
                        } else {
                            gepUsed.add((GEPInstr) ptr);
                        }
                    } else {
                        rootMightUsed.add(((GEPInstr) ptr).getRoot());
                    }
                } else if (!(ptr instanceof GlobalObject)) {
                    throw new RuntimeException("这里 load 和 store 的指针应该只能是 gep 或者全局对象");
                }
            }
            instruction = (Instruction) instruction.getNext();
        }
        
        HashSet<BasicBlock> idoms = basicBlock.getIDoms();
        HashMap<GEPInstr, Value> nextDefMap = null;
        if (idoms.isEmpty()) {
            return;
        } else if (idoms.size() == 1) {
            boolean onlyYou = true;
            for (BasicBlock idom : idoms) {
                HashSet<BasicBlock> pres = idom.getPres();
                for (BasicBlock pre : pres) {
                    if (!pre.equals(basicBlock)) {
                        onlyYou = false;
                        break;
                    }
                }
            }
            if (onlyYou) {
                nextDefMap = defMap;
            }
        }
        
        for (BasicBlock next : idoms) {
            if (nextDefMap == null) {
                dfsIdoms(next, new HashMap<>());
            } else {
                dfsIdoms(next, nextDefMap);
            }
        }
    }
}
