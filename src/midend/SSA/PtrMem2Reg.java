package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.convop.Bitcast;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import frontend.ir.structure.GlobalObject;

import java.util.*;

/**
 * 针对指针的 mem2reg，可以认为是在做别名分析
 * 现在的想法是按照控制树 dfs，保存对于一个地址的赋值记录，如果 dom 分叉（直接控制块不止一个）则清空当前列表，以避免多重定义导致冲突。
 * 对于 call 需要特殊处理，函数中可能出现修改指针指向内存内容的操作。
 * 必须在 GVN 之后，因为需要判断是不是同一个指针
 * 还有一个前置任务是要合并 GEP，避免相同指针的表现形式不同导致错误删除和不当替换
 */
public class PtrMem2Reg {
    private static HashSet<String> gepHashUsed;
    private static HashSet<Value> rootsIndefinitelyUsed; // 这些基址对应的任何一个指针都有可能被使用
    private static HashSet<Value> rootsDefinitelyUsed;   // 这些基址对应的某些特定指针会被使用
    private static boolean DONT_MOVE;
    
    public static void execute(ArrayList<Function> functions) {
        if (functions == null) {
            throw new NullPointerException();
        }
        
        gepHashUsed = new HashSet<>();
        rootsIndefinitelyUsed = new HashSet<>();
        rootsDefinitelyUsed = new HashSet<>();
        DONT_MOVE = false;
        
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            dfsIdoms(basicBlock, new HashMap<>(), new HashMap<>());
        }
        
        for (Function function : functions) {
            BasicBlock basicBlock = (BasicBlock) function.getBasicBlocks().getHead();
            while (basicBlock != null) {
                Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
                while (instruction != null) {
                    if (instruction instanceof StoreInstr) {
                        Value ptr = ((StoreInstr) instruction).getPtr();
                        if (ptr instanceof GEPInstr) {
                            boolean mightBeUsed = (rootsDefinitelyUsed.contains(((GEPInstr) ptr).getRoot())
                                    && ((GEPInstr) ptr).hasNonConstIndex());
                            if (!DONT_MOVE && !gepHashUsed.contains(((GEPInstr) ptr).myHash()) &&
                                    !rootsIndefinitelyUsed.contains(((GEPInstr) ptr).getRoot()) &&
                                    !mightBeUsed) {
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
    
    private static void dfsIdoms(BasicBlock basicBlock, HashMap<String, Value> defMap,
                                 HashMap<String, GEPInstr> keyMap) {
        if (basicBlock == null) {
            throw new NullPointerException();
        }
        Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
        while (instruction != null) {
            
            OIS.simpleOIS4ins(instruction);
            
            if (instruction instanceof CallInstr) {
                // todo: 之后要确认一下跟哪些指针有关系。
                List<Value> rParams = ((CallInstr) instruction).getRParams();
                boolean relatedToPtr = false;
                for (Value rp : rParams) {
                    if (rp instanceof GEPInstr || rp instanceof Bitcast) {
                        relatedToPtr = true;
                        break;
                    }
                }
                if (relatedToPtr) {
                    defMap.clear();
                    keyMap.clear();
                    DONT_MOVE = true;
                }
            } else if (instruction instanceof StoreInstr) {
                Value ptr = ((StoreInstr) instruction).getPtr();
                if (ptr instanceof GEPInstr) {
                    // root 只能是 AllocaInstr，GlobalObject，FParam 三种
                    Value root = ((GEPInstr) ptr).getRoot();
                    if (((GEPInstr) ptr).hasNonConstIndex()) {
                        Iterator<String> it = defMap.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            if (keyMap.get(key).getRoot().equals(root)) {
                                it.remove();
                                keyMap.remove(key);
                            }
                        }
                    } else {
                        String key = ((GEPInstr) ptr).myHash();
                        defMap.put(key, ((StoreInstr) instruction).getValue());
                        keyMap.put(key, (GEPInstr) ptr);
                    }
                } else if (!(ptr instanceof GlobalObject)) {
                    throw new RuntimeException("这里 load 和 store 的指针应该只能是 gep 或者全局对象");
                }
            } else if (instruction instanceof LoadInstr) {
                Value ptr = ((LoadInstr) instruction).getPtr();
                if (ptr instanceof GEPInstr) {
                    if (!((GEPInstr) ptr).hasNonConstIndex()) {
                        Value defVal = defMap.get(((GEPInstr) ptr).myHash());
                        if (defVal != null) {
                            instruction.replaceUseTo(defVal);
                            instruction.removeFromList();
                        } else {
                            gepHashUsed.add(((GEPInstr) ptr).myHash());
                            rootsDefinitelyUsed.add(((GEPInstr) ptr).getRoot());
                        }
                    } else {
                        rootsIndefinitelyUsed.add(((GEPInstr) ptr).getRoot());
                    }
                } else if (!(ptr instanceof GlobalObject)) {
                    throw new RuntimeException("这里 load 和 store 的指针应该只能是 gep 或者全局对象");
                }
            }
            instruction = (Instruction) instruction.getNext();
        }
        
        HashSet<BasicBlock> idoms = basicBlock.getIDoms();
        HashMap<String, Value> nextDefMap = null;
        HashMap<String, GEPInstr> nextKeyMap = null;
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
                nextKeyMap = keyMap;
            }
        }
        
        for (BasicBlock next : idoms) {
            if (nextDefMap == null) {
                dfsIdoms(next, new HashMap<>(), new HashMap<>());
            } else {
                dfsIdoms(next, nextDefMap, nextKeyMap);
            }
        }
    }
}
