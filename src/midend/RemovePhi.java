package midend;

import debug.DEBUG;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.EmptyInstr;
import frontend.ir.instr.otherop.MoveInstr;
import frontend.ir.instr.otherop.PCInstr;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class RemovePhi {
    private static int blkCnt = 0;

    public static void phi2move(HashSet<Function> functions) {
        for (Function function : functions) {
            removePhi(function);
            addMove(function);
        }
    }

    private static void addMove(Function function) {
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            Instruction last = (Instruction) blk.getInstructions().getTail();
            assert last != null;
            if (last.getPrev() == null || !(last.getPrev() instanceof PCInstr)) {
                blk = (BasicBlock) blk.getNext();
                continue;
            }
            //只有一个PCInstr
            //TODO:setUse and removeFromList
            Instruction instr = (Instruction) last.getPrev();
            ArrayList<Value> srcs = ((PCInstr) instr).getSrcs();
            ArrayList<Value> dsts = ((PCInstr) instr).getDsts();
            HashSet<Value> srcSet = new HashSet<>(((PCInstr) instr).getSrcs());
            HashSet<Value> dstSet = new HashSet<>(((PCInstr) instr).getDsts());

            for (int i =0; i < srcs.size(); i++){
                Value src = srcs.get(i);
                Value dst = dsts.get(i);
                if (src == dst) {
                    continue;
                }
                if (!srcSet.contains(dst)) {
                    //TODO:insertBefore and insertAfter should be set used
                    MoveInstr move = new MoveInstr(src, dst);
                    move.insertBefore(blk.getEndInstr());
                    srcSet.remove(src);
                    dstSet.remove(dst);
                    srcs.remove(i);
                    dsts.remove(i);
                    i--;
                } else {
                    //src->dst, dst->c => dst =>dst'
                    // a->b, b->c => b->b' a->b b'->c
                    Instruction tmp = new EmptyInstr();
                    MoveInstr move1 = new MoveInstr(dst, tmp);
                    move1.insertBefore(blk.getEndInstr());
                    MoveInstr move2 = new MoveInstr(src, dst);
                    move2.insertBefore(blk.getEndInstr());
                    srcSet.remove(src);
                    srcSet.remove(dst);
                    srcSet.add(tmp);
                    dstSet.remove(dst);
                    for (int j = i; j < srcs.size(); j++) {
                        if (srcs.get(j) == dst) {
                            srcs.remove(j);
                            srcs.add(j, tmp);
                        }
                    }
                    srcs.remove(i);
                    dsts.remove(i);
                    i--;
                }
            }
            instr.removeFromList();
            // a->b, b->c => a->b' b->c b'->b? or replace all b => b'
//            blk = (BasicBlock) blk.getNext();
        }
    }

    private static void removePhi(Function function) {
        blkCnt = ((BasicBlock)function.getBasicBlocks().getTail()).getLabelCnt() + 1;
        BasicBlock blk = (BasicBlock) function.getBasicBlocks().getHead();
        while (blk != null) {
            if (!(blk.getInstructions().getHead() instanceof PhiInstr)) {
                blk = (BasicBlock) blk.getNext();
                continue;
            }
            Instruction instr = (Instruction) blk.getInstructions().getHead();
            while (instr instanceof PhiInstr) {
                PhiInstr phi = (PhiInstr) instr;
                for (int i = 0; i < phi.getPrtBlks().size(); i++) {
                    BasicBlock pre = phi.getPrtBlks().get(i);
                    Value src = phi.getValues().get(i);
                    if (pre.getSucs().size() == 1) {
                        Instruction last = pre.getEndInstr();
                        if ((last.getPrev()) instanceof PCInstr) {
                            ((PCInstr) last.getPrev()).addPC(src, phi);
                        } else {
                            PCInstr pc = new PCInstr();
                            pc.addPC(src, phi);
                            pc.insertBefore(last);
                        }
                    } else if (pre.getSucs().size() == 2){
                        BranchInstr branch = (BranchInstr) pre.getEndInstr();
                        BasicBlock newBlk = new BasicBlock(pre.getDepth());
                        newBlk.insertAfter(pre);
                        branch.modifyUse(blk, newBlk);
                        newBlk.setLabelCnt(blkCnt++);
                        PCInstr pc = new PCInstr();
                        pc.addPC(src, phi);
                        newBlk.addInstruction(pc);
                        newBlk.addInstruction(new JumpInstr(blk));
                    } else if (pre.getSucs().size() != 0){
                        DEBUG.dbgPrint(pre.value2string());
                        DEBUG.dbgPrint2("sucs size: " + pre.getSucs());
                        throw new RuntimeException("too many sucs");
                    }
                }
                phi.removeFromList();
                instr = (Instruction) instr.getNext();
            }
        }
    }
}
