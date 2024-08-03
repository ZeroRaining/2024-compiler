package midend.loop;

import Utils.CustomList;
import backend.itemStructure.Group;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.binop.BinaryOperation;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.otherop.cmp.Cmp;
import frontend.ir.instr.otherop.cmp.CmpCond;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import frontend.ir.structure.GlobalObject;
import frontend.ir.structure.Procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class LoopUnroll {
    private static int codeSize = 3000;
    public static void execute(ArrayList<Function> functions) {
        for (Function function : functions) {
            AnalysisLoop.LoopIndVars(function);
            SimpleLoopUnroll(function);
        }
    }

    private static void SimpleLoopUnroll(Function function) {
        Iterator<Loop> loopIterator = function.getOuterLoop().iterator();
        while (loopIterator.hasNext()) {
            Loop loop = loopIterator.next();
            if (dfs4LoopUnroll(loop)) {
                loopIterator.remove();
                function.getAllLoop().remove(loop);
                function.getHeader2loop().remove(loop.getHeader());
                //remove from outLoop in function
            }
        }
//        for (int i = 0; i)
    }

    private static boolean dfs4LoopUnroll(Loop loop) {
        Iterator<Loop> loopIterator = loop.getInnerLoops().iterator();
        Function function = ((Procedure) loop.getHeader().getParent().getOwner()).getParentFunc();
        while (loopIterator.hasNext()) {
            Loop inner = loopIterator.next();
            if (dfs4LoopUnroll(inner)) {
                loopIterator.remove();
                function.getAllLoop().remove(loop);
                function.getHeader2loop().remove(loop.getHeader());
                //remove from outLoop in function
                for (Loop inInner : inner.getInnerLoops()) {
                    inner.setPrtLoop(loop);
                }
            }
        }
        if (!loop.hasIndVar()) {
            return false;
        }
        Value itInit = loop.getBegin();
        Value itStep = loop.getStep();
        Value itEnd = loop.getEnd();
        if (!(itInit instanceof ConstValue) || !(itStep instanceof ConstValue) || !(itEnd instanceof ConstValue)) {
            return false;
        }
        BinaryOperation itAlu = loop.getAlu();
        Cmp condInstr = loop.getCond();
        //如果是浮点数？？？
        int init = itInit.getNumber().intValue();
        int end = itEnd.getNumber().intValue();
        int step = itStep.getNumber().intValue();
        if (step == 0) {
            throw new RuntimeException("what happened? Bro!");
        }
        int times = calculateLoopTimes(itAlu.getOperationName(), condInstr.getCond(), init, step, end);
        int cnt = 0;
        for (BasicBlock blk : loop.getBlks()) {
            cnt += blk.getInstructions().getSize();
        }
        if (cnt * times > codeSize) {
            return false;
        }
        Unroll(loop, times);
        return true;
    }

    private static void Unroll(Loop loop, int times) {
        HashMap<Value, Value> phiInHead = new HashMap<>();//各个latch到head的phi的取值
        HashMap<Value, Value> begin2end = new HashMap<>();//head中的value被映射的值，维护LCSSA
        ArrayList<PhiInstr> headPhis = new ArrayList<>();
        BasicBlock header = loop.getHeader();
        CustomList.Node instr = header.getInstructions().getHead();
        while (instr instanceof PhiInstr) {
            headPhis.add((PhiInstr) instr);
            instr = instr.getNext();
        }

        //
        BasicBlock exit = loop.getExits().get(0);
        BasicBlock latch = loop.getLatchs().get(0);
        BasicBlock headerNext = null;
        for (BasicBlock blk : header.getSucs()) {
            if (blk != exit) {
                headerNext = blk;
            }
        }
        if (headerNext == null) {
            throw new RuntimeException("header only has exit as sucs");
        }
        for (PhiInstr phi : headPhis) {
            int index = phi.getPrtBlks().indexOf(latch);
            Value newValue = phi.getValues().get(index);
            phi.getPrtBlks().remove(index);
            phi.getValues().remove(index);
            phiInHead.put(phi, newValue);
            begin2end.put(phi, newValue);
        }

        BranchInstr br = (BranchInstr) header.getEndInstr();
        br.removeFromList();
        header.setRet(false);
        header.addInstruction(new JumpInstr(headerNext));
        latch.getInstructions().getTail().removeFromList();
        latch.setRet(false);

        //获得dfs顺序？
        //假如有循环不能展开？
        Procedure procedure = (Procedure) header.getParent().getOwner();
//        Function function = procedure.getParentFunc();
//        Loop prtLoop = loop.getPrtLoop();
//        if (prtLoop != null) {
//            prtLoop.getInnerLoops().remove(loop);
//            for (Loop inner : loop.getInnerLoops()) {
//                inner.setPrtLoop(prtLoop);
//            }
//        }

        old2new = new HashMap<>(phiInHead);
        Group<BasicBlock, BasicBlock> oneLoop = new Group<>(headerNext, latch);
        for (int i = 0; i < times - 1; i++) {
            oneLoop = cloneBlks(oneLoop, procedure, begin2end, phiInHead);
        }

        BasicBlock lastLatch = oneLoop.getSecond();
        lastLatch.addInstruction(new JumpInstr(exit));

        Instruction phi = (Instruction) exit.getInstructions().getHead();
        while (phi instanceof PhiInstr) {
            for (Value value : ((PhiInstr) phi).getValues()) {
                if (value instanceof Instruction && ((Instruction) value).getParentBB() == header) {
                    phi.modifyUse(value, begin2end.get(value));
                    ((PhiInstr) phi).modifyPrtBlk(header, lastLatch);
                }
            }
            phi = (Instruction) phi.getNext();
        }

    }
    private static HashMap<Value, Value> old2new;

    private static Group<BasicBlock, BasicBlock> cloneBlks(Group<BasicBlock, BasicBlock> oneLoop, Procedure procedure,
                                                           HashMap<Value, Value> begin2end, HashMap<Value, Value> phiInHead) {
        ArrayList<BasicBlock> newBlks = new ArrayList<>();

        BasicBlock curBB = oneLoop.getFirst();
        BasicBlock stop = (BasicBlock) oneLoop.getSecond().getNext();
        while (curBB != stop) {
            BasicBlock newBB = new BasicBlock(curBB.getLoopDepth(), procedure.getAndAddBlkIndex());
            old2new.put(curBB, newBB);
            newBlks.add(newBB);

            Instruction curIns = (Instruction) curBB.getInstructions().getHead();
            while (curIns != null) {
                Instruction newIns = curIns.cloneShell(procedure);
                newBB.addInstruction(newIns);
                old2new.put(curIns, newIns);
                curIns = (Instruction) curIns.getNext();
            }

            curBB = (BasicBlock) curBB.getNext();
        }

        BasicBlock last = oneLoop.getSecond();

        for (BasicBlock newBB : newBlks) {
            Instruction newIns = (Instruction) newBB.getInstructions().getHead();
            while (newIns != null) {
                if (newIns instanceof PhiInstr) {
                    ((PhiInstr) newIns).renewBlocks(old2new);
                }

                ArrayList<Value> usedValues = new ArrayList<>(newIns.getUseValueList());
                for (Value toReplace : usedValues) {
                    if (!old2new.containsKey(toReplace)) {
                        if (newIns instanceof ReturnInstr && toReplace == newBB) {
                            continue;
                        }
//                        if (!(toReplace instanceof ConstValue) && !(toReplace instanceof GlobalObject)) {
//                            throw new RuntimeException("使用了未曾设想的 value " + toReplace.toString());
//                        }
                    } else {
                        newIns.modifyUse(toReplace, old2new.get(toReplace));
                    }
                }
                newIns = (Instruction) newIns.getNext();
            }

            newBB.insertAfter(last);
            last = newBB;
        }
        for (Value key : begin2end.keySet()) {
            Value oldValue = begin2end.get(key);
            Value newValue;
            if (oldValue instanceof ConstValue) {
                newValue = oldValue;
            } else newValue = old2new.getOrDefault(oldValue, oldValue);
            begin2end.put(key, newValue);
        }

        Group<BasicBlock, BasicBlock> newOneLoop = new Group<>(newBlks.get(0), newBlks.get(newBlks.size() - 1));

        oneLoop.getSecond().addInstruction(new JumpInstr(newOneLoop.getFirst()));

        return newOneLoop;
    }

    private static int calculateLoopTimes(String alu, CmpCond cond, int init, int step, int end) {
        if (alu.contains("add")) {
            switch (cond) {
                case EQ:
                    return init == end ? 1 : 0;
                case NE:
                    return ((end - init) % step == 0) ? (end - init) / step : -1;
                case GE, LE:
                    return (end - init) / step + 1;
                case GT, LT:
                    return ((end - init) % step == 0)? (end - init) / step : (end - init) / step + 1;
                default:
                    throw new RuntimeException("what happened? Bro!");
            }
        } else if (alu.contains("sub")) {
            switch (cond) {
                case EQ:
                    return init == end ? 1 : 0;
                case NE:
                    return ((init - end) % step == 0)? (init - end) / step : -1;
                case GE, LE:
                    return (init - end) / step + 1;
                case GT, LT:
                    return ((init - end) % step == 0)? (init - end) / step : (init - end) / step + 1;
                default:
                    throw new RuntimeException("what happened? Bro!");
            }
        } else if (alu.contains("mul")) {
            double val = Math.log(end / init) / Math.log(step);
            boolean tag = init * Math.pow(step, val) == end;
            switch (cond) {
                case EQ:
                    return init == end ? 1 : 0;
                case NE:
                    return tag ? (int) val : -1;
                case GE, LE:
                    return (int) val + 1;
                case GT, LT:
                    return tag ? (int) val : (int) val + 1;
                default:
                    throw new RuntimeException("what happened? Bro!");
            }
        }
        return -1;
    }

}
