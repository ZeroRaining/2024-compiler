package midend.loop;

import Utils.CustomList;
import backend.itemStructure.Group;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstBool;
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
import frontend.ir.structure.Procedure;
import midend.SSA.DeadCodeRemove;
import midend.SSA.MergeBlock;
import midend.SSA.OIS;

import java.util.ArrayList;
import java.util.HashMap;

public class LoopLift {
    public static void execute(ArrayList<Function> functions) {
        AnalysisLoop.execute(functions);
        for (Function function : functions) {
            // 要求在完全内联之后，只对 main 函数做循环展开，递归函数不做展开
            if (function.isMain()) {
                AnalysisLoop.LoopIndVars(function);
                loopLift(function);
            }
        }
    }

    private static void loopLift(Function function) {
        for (Loop outer : function.getOuterLoop()) {
            lift(outer);
        }
    }

    private static void lift(Loop loop) {
        if (loop.getInnerLoops().isEmpty()) {
            return;
        }
        for (Loop inner : loop.getInnerLoops()) {
            if (!inner.getInnerLoops().isEmpty()) {
                //TODO: to be implemented
                return;
            }
            if (loopCanLift(inner)) {
                UnrollOnce(loop, inner);
            }
        }
    }

    private static HashMap<Value, Value> old2new;

    private static void UnrollOnce(Loop loop, Loop inner) {
        if (!loop.hasIndVar()) {
            return;
        }
        Value init = loop.getBegin();
        Value end = loop.getEnd();
        if (!(init instanceof ConstValue)) {
            return;
        }
        if (!(end instanceof ConstValue)) {
            return;
        }
        BinaryOperation alu = loop.getAlu();
        Cmp cmp = loop.getCond();
        if (alu.getOperationName().equals("add") && cmp.getCond().equals(CmpCond.LT)) {
            if (init.getNumber().intValue() >= end.getNumber().intValue()) {
                return;
            }
        } else if (alu.getOperationName().equals("sub") && cmp.getCond().equals(CmpCond.GT)) {
            if (init.getNumber().intValue() <= end.getNumber().intValue()) {
                return;
            }
        } else {
            //TODO implement other cmp
            return;
        }
//        throw new RuntimeException("this func may have problem");
        loop.LoopPrint();
        //循环一定会被执行一次
        LoopUnroll.setIsLift(false);

        HashMap<PhiInstr, Value> phiInHead = new HashMap<>();//各个latch到head的phi的取值
        HashMap<Value, Value> begin2end = new HashMap<>();//head中的value被映射的值，维护LCSSA
        ArrayList<PhiInstr> headPhis = new ArrayList<>();
        BasicBlock header = loop.getHeader();
        CustomList.Node instr = header.getInstructions().getHead();
        while (instr instanceof PhiInstr) {
            headPhis.add((PhiInstr) instr);
            instr = instr.getNext();
        }
//        loop.LoopPrint();
        BasicBlock loopExit = loop.getExits().get(0);
        BasicBlock latch = loop.getLatchs().get(0);

        for (PhiInstr phi : headPhis) {
            int index = phi.getPrtBlks().indexOf(latch);
            Value newValue = phi.getValues().get(index);
            //先保留phi的value
            phiInHead.put(phi, newValue);
            begin2end.put(newValue, newValue);
        }
        //TODO：复制后删掉修改跳转


        BasicBlock innerPreHeader = inner.getPreHeader();
        if (innerPreHeader.getPres().size() != 1) {
            return;
        }
        BasicBlock entry2preHeader =null;
        for (BasicBlock block : innerPreHeader.getPres()) {
            entry2preHeader = block;
        }
        if (entry2preHeader == null) {
            return;
        }
        Instruction endHeaderIns = entry2preHeader.getEndInstr();
        if (endHeaderIns instanceof JumpInstr) {
            return;
        }
        if (!(endHeaderIns instanceof BranchInstr)) {
            return;
        }

        ConstBool fa = new ConstBool(0);

        Value oldHeaderCond = ((BranchInstr) endHeaderIns).getCond();
//        endHeaderIns.modifyUse(oldHeaderCond, fa);


        Instruction endLarchIns = entry2preHeader.getEndInstr();
        if (endLarchIns instanceof JumpInstr) {
            return;
        }
        if (!(endLarchIns instanceof BranchInstr)) {
            return;
        }


        Value oldLatchCond = ((BranchInstr) endLarchIns).getCond();
//        endLarchIns.modifyUse(oldLatchCond, fa);


        Procedure procedure = (Procedure) header.getParent().getOwner();
        //clone
        old2new = new HashMap<>();
        old2new.put(loop.getPreHeader(), latch);
        Group<BasicBlock, BasicBlock> oneLoop = new Group<>(header, latch);
        oneLoop = cloneBlks(oneLoop, procedure, begin2end, loop.getPrtLoop(), inner);

        latch.getEndInstr().modifyUse(header, oneLoop.getFirst());

        BasicBlock beginBlk = innerPreHeader;
        BasicBlock endBlk = inner.getLatchs().get(0); ///latch.size?

        BasicBlock next = (BasicBlock) old2new.get(endBlk).getNext();

        BasicBlock tmp = (BasicBlock) beginBlk.getNext();
        while (tmp != endBlk.getNext()) {
            Instruction instruction = (Instruction) tmp.getInstructions().getHead();
            while (instruction != null) {
                old2new.get(instruction).replaceUseTo(instruction);
                instruction = (Instruction) instruction.getNext();
            }
//            old2new.get(tmp).removeFromList();
            tmp = (BasicBlock) tmp.getNext();
        }

        BasicBlock newEntry = (BasicBlock) old2new.get(entry2preHeader);
        System.out.println("+++++" + newEntry.getEndInstr().print());
        newEntry.getEndInstr().removeFromList();
        newEntry.setRet(false);
        newEntry.addInstruction(new JumpInstr(next));


//        latch.getInstructions().getTail().removeFromList();
//        latch.setRet(false);
//        latch.addInstruction(new JumpInstr(oneLoop.getFirst()));
        for (PhiInstr phi : headPhis) {
            phi.removeUse(phiInHead.get(phi));
        }

        //维护循环块

        BasicBlock lastLatch = oneLoop.getSecond();
        BasicBlock preHeader = loop.getPreHeader();
        BasicBlock newHeader = oneLoop.getFirst();

        for (PhiInstr oldPhi : phiInHead.keySet()) {
            PhiInstr newPhi = (PhiInstr) old2new.get(oldPhi);
            int index = newPhi.getPrtBlks().indexOf(latch);
            newPhi.modifyUse(newPhi.getValues().get(index), phiInHead.get(oldPhi));
        }

        Instruction phi = (Instruction) loopExit.getInstructions().getHead();
        while (phi instanceof PhiInstr) {
            int size = ((PhiInstr) phi).getValues().size();
            for (int i = 0; i < size; i++) {
                Value value = ((PhiInstr) phi).getValues().get(i);
//                if (value instanceof Instruction) {
                ((PhiInstr) phi).addUse(old2new.get(value), lastLatch);
//                }
            }
            phi = (Instruction) phi.getNext();
        }
//        OIS.OSI4blks(header, oneLoop.getSecond());
//        DeadCodeRemove.removeCode(header, oneLoop.getSecond());
//        MergeBlock.merge4loop(loop.getPrtLoop(), header, oneLoop.getSecond());
    }

    private static boolean loopCanLift(Loop loop) {
        if (!loop.hasIndVar()) {
            return false;
        }
        Value init = loop.getBegin();
        Value end = loop.getEnd();
        Value step = loop.getEnd();
        if (init instanceof Instruction) {
            if (loop.getPrtLoop().getBlks().contains(((Instruction) init).getParentBB())) {
                return false;
            }
        }
        if (end instanceof Instruction) {
            if (loop.getPrtLoop().getBlks().contains(((Instruction) end).getParentBB())) {
                return false;
            }
        }
        if (step instanceof Instruction) {
            if (loop.getPrtLoop().getBlks().contains(((Instruction) step).getParentBB())) {
                return false;
            }
        }
        return true;
    }

    private static Group<BasicBlock, BasicBlock> cloneBlks(Group<BasicBlock, BasicBlock> oneLoop, Procedure procedure,
                                                           HashMap<Value, Value> begin2end, Loop prtLoop, Loop inner) {
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
                old2new.putIfAbsent(curIns, newIns);
                curIns = (Instruction) curIns.getNext();
            }

            curBB = (BasicBlock) curBB.getNext();
        }

        BasicBlock last = oneLoop.getSecond();

        for (BasicBlock newBB : newBlks) {
            if (prtLoop != null) {
                prtLoop.addBlk(newBB);
            }
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
}
