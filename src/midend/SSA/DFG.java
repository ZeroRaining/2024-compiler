package midend.SSA;

import Utils.CustomList;
import frontend.ir.Use;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.*;

public class DFG {
    private HashSet<Function> functions;
    public DFG(HashSet<Function> functions) {
        this.functions = functions;
        for (Function function : functions) {
            removeBlk(function);
            makeDoms(function);
            makeIDoms(function);
            makeDF(function);
        }
    }

    private void makeDF(Function function) {
        for (CustomList.Node item : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) item;
            HashSet<BasicBlock> DF = new HashSet<>();
            for (CustomList.Node item2 : function.getBasicBlocks()) {
                BasicBlock other = (BasicBlock) item2;
                for (BasicBlock tmp : other.getPres()) {
                    if (block.getDoms().contains(tmp) &&
                            (block.equals(other) || !block.getDoms().contains(other))){
                        DF.add(other);
                        break;
                    }
                }
            }
            block.setDF(DF);
        }
    }


    //b1 -> b2 -> b3
    private void makeIDoms(Function function) {
        for (CustomList.Node item : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) item;
            HashSet<BasicBlock> iDoms = new HashSet<>();
            for (BasicBlock dom1 : block.getDoms()) {
                if (AIDomB(block, dom1)) {
                    iDoms.add(dom1);
                }
            }
            block.setIDoms(iDoms);
        }
    }
    private boolean AIDomB(BasicBlock A, BasicBlock B) {
        HashSet<BasicBlock> ADoms = A.getDoms();
        if (!ADoms.contains(B)) {
            return false;
        }
        if (A.equals(B)) {
            return false;
        }
        for (BasicBlock temp: ADoms) {
            if (!temp.equals(A) && !temp.equals(B)) {
                if (temp.getDoms().contains(B)) {
                    return false;
                }
            }
        }
        return true;
    }
    private void dfs(BasicBlock bb, BasicBlock not,HashSet<BasicBlock> know) {
        if (bb.equals(not)) {
            return;
        }
        if (know.contains(bb)) {
            return;
        }
        know.add(bb);
        for (BasicBlock next: bb.getSucs()) {
            if (!know.contains(next) && !next.equals(not)) {
                dfs(next, not, know);
            }
        }
    }
//    private void makeSingleFuncDom(Function function) {
//        BasicBlock enter = (BasicBlock) function.getBasicBlocks().getHead();
//        BasicBlock bb = enter;
//        for (;bb != null;bb = (BasicBlock) bb.getNext()) {
//            HashSet<BasicBlock> doms = new HashSet<>();//所有的直接后继
//            HashSet<BasicBlock> know = new HashSet<>();
//            dfs4dom(enter, bb, know);
//            for (BasicBlock temp = enter;temp != null;temp = (BasicBlock) temp.getNext()) {
//                if (!know.contains(temp)) {
//                    doms.add(temp);
//                }
//            }
//
//            bb.setDoms(doms);
//        }
//    }

    private void makeDoms(Function function) {
        BasicBlock firstBlk = (BasicBlock) function.getBasicBlocks().getHead();
        for (CustomList.Node item : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) item;
            HashSet<BasicBlock> independent = new HashSet<>();
            dfs4dom(firstBlk, block, independent);

            HashSet<BasicBlock> doms = new HashSet<>();
            for (CustomList.Node node : function.getBasicBlocks()) {
                BasicBlock otherBlk = (BasicBlock) node;
                if (!independent.contains(otherBlk)) {
                    doms.add(otherBlk);
                }
            }
            block.setDoms(doms);
        }
    }

    private void dfs4dom(BasicBlock block, BasicBlock otherBlk, HashSet<BasicBlock> independent) {
        if (block.equals(otherBlk)) {
            return;
        }
        if (independent.contains(otherBlk)) {
            return;
        }
        independent.add(block);
        for (BasicBlock next : block.getSucs()) {
            if (!independent.contains(next) && next != otherBlk){
                dfs4dom(next, otherBlk, independent);
            }
        }
    }

    private void dfs4remove(BasicBlock block) {
        if (block.getBeginUse() == null) {
            Instruction instr = block.getEndInstr();
            block.removeFromList();
            if (instr instanceof JumpInstr) {
                dfs4remove(((JumpInstr) instr).getTarget());
            } else if (instr instanceof BranchInstr) {
                dfs4remove(((BranchInstr) instr).getThenTarget());
                dfs4remove(((BranchInstr) instr).getElseTarget());
            }
        }
    }

    private void removeBlk(Function function) {
        BasicBlock firstBlk = (BasicBlock) function.getBasicBlocks().getHead();
        HashMap<BasicBlock, HashSet<BasicBlock>> pres = new HashMap<>();
        HashMap<BasicBlock, HashSet<BasicBlock>> sucs = new HashMap<>();
//        while (firstBlk != null) {
//            dfs4remove(firstBlk);
//            firstBlk = (BasicBlock) firstBlk.getNext();
//        }
        BasicBlock tmpBlk = (BasicBlock) function.getBasicBlocks().getHead();
        while (tmpBlk != null) {
            pres.put(tmpBlk, new HashSet<>());
            sucs.put(tmpBlk, new HashSet<>());
            tmpBlk = (BasicBlock) tmpBlk.getNext();
        }

        Iterator<CustomList.Node> blks = function.getBasicBlocks().iterator();
        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            Use use = block.getBeginUse();
            for (;use != null; use = (Use) use.getNext()) {
                BasicBlock user = use.getUser().getParentBB();
                pres.get(block).add(user);
                sucs.get(user).add(block);
            }
        }

        blks = function.getBasicBlocks().iterator();
        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            block.setPres(pres.get(block));
            block.setSucs(sucs.get(block));
        }
    }



}
