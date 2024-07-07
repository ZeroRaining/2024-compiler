package midend.SSA;

import Utils.CustomList;
import debug.DEBUG;
import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
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
            curRegCnt = 0;
            removeAlloc(function);
        }
    }

    private void removeAlloc(Function function) {
        for (CustomList.Node node : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) node;
            for (CustomList.Node item : block.getInstructions()) {
                Instruction instr = (Instruction) item;
                if (instr instanceof AllocaInstr && instr.getPointerLevel() == 1) {
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
        //to be improved
        if (useBlks.isEmpty()) {
            for (Instruction ins : defIns) {
                ins.removeFromList();
            }
        } else if (defIns.size() == 1 && defBlks.get(0).getDoms().containsAll(useBlks)) {
            //不处理，未定义的初始值
            Instruction store = defIns.get(0);//store指令
            assert store instanceof StoreInstr;
            Value toStoreValue = ((StoreInstr) store).getValue();//要被使用的值1
            for (Instruction load : useIns) {//load指令
                load.modifyAllUseThisToUseA(toStoreValue);
            }
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
                defIns.add(phi);
                useBlks.add(block);
                defBlks.add(block);
            }

            BasicBlock block = (BasicBlock) instr.getParentBB().getParent().getHead();
            Stack<Value> S = new Stack<>();
            dfs4rename(S, block, useIns, defIns);
        }

        for (Instruction ins : useIns) {
            if (!(ins instanceof PhiInstr)) {
                ins.removeFromList();
            }
        }
        for (Instruction ins : defIns) {
            if (!(ins instanceof PhiInstr)) {
                ins.removeFromList();
            }
        }
        instr.removeFromList();
    }

    private void dfs4rename(Stack<Value> S, BasicBlock now, ArrayList<Instruction> useIns, ArrayList<Instruction> defIns) {
        int cnt = 0;
        for (CustomList.Node item : now.getInstructions()) {
            Instruction instr = (Instruction) item;
            if (!(instr instanceof PhiInstr) && useIns.contains(instr)) {
                //changeValue
                assert instr instanceof LoadInstr;
                instr.modifyAllUseThisToUseA(getTopOfStack(S));
            }
            if (defIns.contains(instr)) {
                assert instr instanceof StoreInstr || instr instanceof PhiInstr;
                if (instr instanceof StoreInstr) {
                    S.push(((StoreInstr) instr).getValue());
                } else {
                    S.push(instr);
                }
                cnt++;
            }
        }
        HashSet<BasicBlock> sucs = now.getSucs();
        for (BasicBlock block : sucs) {
            for (CustomList.Node item : now.getInstructions()) {
                Instruction instr = (Instruction) item;
                if (!(instr instanceof PhiInstr)) {
                    break;
                }
                if (useIns.contains(instr)) {
                    instr.modifyUse(now, getTopOfStack(S));
                }
            }
        }

        for (BasicBlock nextBlk : now.getIDoms()) {
            dfs4rename(S, nextBlk, useIns, defIns);
        }

        for (int i = 0; i < cnt; i++) {
            S.pop();
        }
    }

    public Value getTopOfStack(Stack<Value> S) {
        if (S.isEmpty()) {
            return new ConstInt(0);
        } else{
            return S.peek();
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
