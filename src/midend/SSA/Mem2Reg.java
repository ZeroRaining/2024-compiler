package midend.SSA;

import Utils.CustomList;
import debug.DEBUG;
import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.AllocaInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.*;

public class Mem2Reg {
    private HashSet<Function> functions;
    private int curRegCnt = 0;//TODO:!!!!!
    public Mem2Reg(HashSet<Function> functions) {
        this.functions = functions;
        for (Function function : functions) {
            removeAlloc(function);
        }
    }

    private void removeAlloc(Function function) {
        for (CustomList.Node node : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) node;
            for (CustomList.Node item : block.getInstructions()) {
                Instruction instr = (Instruction) item;
                if (instr instanceof AllocaInstr) {
                    remove(instr);
                }
            }
        }
    }

    private void remove(Instruction instr) {
        ArrayList<BasicBlock> defBlks = new ArrayList<>();
        ArrayList<BasicBlock> useBlks = new ArrayList<>();
        ArrayList<Instruction> defIns = new ArrayList<>();
        ArrayList<Instruction> useIns = new ArrayList<>();
//        visited = new HashMap<>();
//        placed = new HashMap<>();
        useDBG(instr);
        Use use = instr.getBeginUse();
        for (; use != null; use = (Use) use.getNext()) {
            Instruction ins = use.getUser();
            BasicBlock blk = ins.getParentBB();
            if (ins instanceof StoreInstr) {
                defBlks.add(blk);
                defIns.add(ins);
            } else if (ins instanceof LoadInstr) {
                useBlks.add(blk);
                useIns.add(ins);
            } else {
                throw new RuntimeException("Furina said you are wrong!");
            }
        }

        if (useBlks.isEmpty()) {
            for (Instruction ins : defIns) {
                ins.removeFromList();
            }
        } else if (defIns.size() == 1 && defBlks.get(0).getDoms().containsAll(useBlks)) {
            //不处理，未定义的初始值
            BasicBlock def = defBlks.get(0);
            Instruction store = defIns.get(0);//store指令
            assert store instanceof StoreInstr;
            Value toStoreValue = ((StoreInstr) store).getValue();//要被使用的值1
            for (Instruction load : useIns) {//load指令
                Use use1 = load.getBeginUse();//使用load的值
                //TODO：将使用load的值，变为storeValue
                while (use1 != null) {
                    Use nextUse = (Use) use1.getNext();
                    Instruction instrUseLoad = use1.getUser();
                    use1.removeFromList();//
                    instrUseLoad.removeUse(use1);
                    instrUseLoad.setUse(toStoreValue);
                    use1 = nextUse;
                    instrUseLoad.modifyValue(load, toStoreValue);
                }
            }
            for (Instruction ins : useIns) {
                if (!(ins instanceof PhiInstr)) {
                    ins.removeFromList();
                }
            }
            for (Instruction ins : defIns) {
                ins.removeFromList();
            }
            instr.removeFromList();
        } else {
            ArrayList<BasicBlock> toPuts = new ArrayList<>();
            Queue<BasicBlock> W = new LinkedList<>(defBlks);
            while (!W.isEmpty()) {
                BasicBlock X = W.poll();
                for (BasicBlock Y : X.getDF()) {
                    if (!toPuts.contains(Y)) {
                        toPuts.add(Y);
                        if (!defBlks.contains(Y)) {
                            W.add(Y);
                        }
                    }
                }
            }
//            if (!worklist.isEmpty()) {
//                BasicBlock X = worklist.poll();
//                for (BasicBlock Y : X.getDF()) {
//                    if (!placed.get(Y)) {
//                        placed.put(Y, true);
//                        //Y处放至<< V <- phi(V1,V2,...) >>
//                        toPuts.add(Y);
//                        if (!visited.get(Y)) {
//                            visited.put(Y, true);
//                            worklist.offer(Y);
//                        }
//                    }
//                }
//            }
            ArrayList<Value> values = new ArrayList<>();
            ArrayList<BasicBlock> prtBlks = new ArrayList<>();
            for (Instruction ins : defIns) {
                values.add(ins);
                prtBlks.add(ins.getParentBB());
            }
            for (BasicBlock block : toPuts) {
                Instruction phi = new PhiInstr(curRegCnt++, values, prtBlks,block);
                block.getInstructions().addToHead(phi);
                useIns.add(phi);
                useBlks.add(block);
            }
            for (Instruction ins : useIns) {
                if (!(ins instanceof PhiInstr)) {
                    ins.removeFromList();
                }
            }
            for (Instruction ins : defIns) {
                ins.removeFromList();
            }
            instr.removeFromList();
        }

    }

    private void useDBG(Instruction instr) {
        DEBUG.dbgPrint1(instr.print());
        DEBUG.dbgPrint1("use:");
        for (Use use = instr.getBeginUse(); use != null; use = (Use) use.getNext()) {
            DEBUG.dbgPrint2(use.getUser().print());
        }
    }
}
