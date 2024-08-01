package midend.loop;

import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.Terminator;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import frontend.ir.structure.Procedure;

import java.util.*;

public class LoopInvariantMotion {
    public static void execute(ArrayList<Function> functions) {
        for (Function function : functions) {
            findInvariant(function);
        }
    }

    private static void findInvariant(Function function) {
        for (Loop loop : function.getOuterLoop()) {
            findInvar4loop(loop);
        }
    }

    private static void findInvar4loop(Loop loop) {
        for (Loop inner : loop.getInnerLoops()) {
            findInvar4loop(inner);
        }
        System.out.println(loop.getHeader() + " " + loop.getSameLoopDepth());
        Queue<Instruction> queue = new LinkedList<>();
        HashSet<Value> invariant = new HashSet<>();
        for (BasicBlock block : loop.getSameLoopDepth()) {
            Instruction instr = (Instruction) block.getInstructions().getHead();
            while (instr != null) {
                if (defOutOfLoop(instr, loop, invariant)) {
                    queue.add(instr);
                    invariant.add(instr);
                }
                instr = (Instruction) instr.getNext();
            }
        }
        //TODO: if-do-while 各个blk的归属
        BasicBlock entering = loop.getEntering();
        BasicBlock tmpBlk = new BasicBlock(entering.getLoopDepth(), ((Procedure) entering.getParent().getOwner()).getAndAddBlkIndex());
        while (!queue.isEmpty()) {
            Instruction instr = queue.poll();
            tmpBlk.addInstruction(instr);
            Use use = instr.getBeginUse();
            while (use != null) {
                Instruction user = use.getUser();
                if (defOutOfLoop(user, loop, invariant)) {
                    invariant.add(user);
                    queue.add(user);
                }
                use = (Use) use.getNext();
            }
        }
        //修改跳转指令以及phi，并将该块放入procedure中
        addTmpBlk(entering, tmpBlk, loop.getHeader());
        //更改loop的相关信息:更改entering，修改父循环的blk内容
        loop.setEntering(tmpBlk);
        if (loop.getPrtLoop() != null) {
            loop.getPrtLoop().addBlk(tmpBlk);
        }
    }

    private static void addTmpBlk(BasicBlock entering, BasicBlock tmpBlk, BasicBlock next) {
        entering.getEndInstr().modifyUse(next, tmpBlk);
        tmpBlk.addInstruction(new JumpInstr(next));
        Instruction instr = (Instruction) next.getInstructions().getHead();
        while (instr instanceof PhiInstr) {
            instr.modifyUse(entering, tmpBlk);
            instr = (Instruction) instr.getNext();
        }
        tmpBlk.insertAfter(entering);
    }

    //instr所使用的值是否都来自循环外
    public static boolean defOutOfLoop(Instruction instr, Loop loop, HashSet<Value> invariant) {
        if (!loop.getBlks().contains(instr.getParentBB())) {
            return false;
        }
        //phi指令
        if (instr instanceof LoadInstr || instr instanceof StoreInstr) {
            return false;
        }
        if (instr instanceof CallInstr) {
            //TODO: no side effect should be lift;
            return false;
        }
        if (instr instanceof Terminator) {
            return false;
        }
        for (Value value : instr.getUseValueList()) {
            if (value instanceof ConstValue) {
                continue;
            } else if (invariant.contains(value)) {
                continue;
            } else if (value instanceof Instruction && !loop.getBlks().contains(((Instruction) value).getParentBB())) {
                continue;
            }
            return false;
        }
        return true;
    }
}
