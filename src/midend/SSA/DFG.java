package midend.SSA;

import Utils.CustomList;
import frontend.ir.Use;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.lang.annotation.Target;
import java.util.*;

public class DFG {
    public static void doDFG(HashSet<Function> functions) {
        for (Function function : functions) {
            removeBlk(function);
            makeDoms(function);
            makeIDoms(function);
            makeDF(function);
        }
    }

    private static void makeDF(Function function) {
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
    private static void makeIDoms(Function function) {
        for (CustomList.Node item : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) item;
            HashSet<BasicBlock> iDoms = new HashSet<>();
            for (BasicBlock dom1 : block.getDoms()) {
                boolean flag = block.getDoms().contains(dom1);
                if (block == dom1) {
                    flag = false;
                }
                for (BasicBlock temp: block.getDoms()) {
                    if (!temp.equals(block) && !temp.equals(dom1)) {
                        if (temp.getDoms().contains(dom1)) {
                            flag = false;
                        }
                    }
                }
                if (flag) {
                    iDoms.add(dom1);
                }
            }
            block.setIDoms(iDoms);
        }
    }

    private static void makeDoms(Function function) {
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

    private static void dfs4dom(BasicBlock block, BasicBlock otherBlk, HashSet<BasicBlock> independent) {
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

    private static void dfs4remove(BasicBlock block) {
        if (block.getBeginUse() == null) {
            Instruction instr = block.getEndInstr();
            block.removeFromList();
            //把我用到的所有block的use信息删掉
            if (instr instanceof JumpInstr) {
                dfs4remove(((JumpInstr) instr).getTarget());
            } else if (instr instanceof BranchInstr) {
                dfs4remove(((BranchInstr) instr).getThenTarget());
                dfs4remove(((BranchInstr) instr).getElseTarget());
            }
        }
    }

    private static void removeUseBlock(Instruction instr, BasicBlock usedBlk) {
        Use use = usedBlk.getBeginUse();
        assert use != null;
        assert instr instanceof JumpInstr || instr instanceof BranchInstr;
        while (use != null) {
            if (use.getUser() == instr) {
                use.removeFromList();
            }
            use = (Use) use.getNext();
        }
    }

    private static void removeBlk(Function function) {
        BasicBlock secondBlk = (BasicBlock) function.getBasicBlocks().getHead().getNext();
        HashMap<BasicBlock, HashSet<BasicBlock>> pres = new HashMap<>();
        HashMap<BasicBlock, HashSet<BasicBlock>> sucs = new HashMap<>();

        while (secondBlk != null) {
            dfs4remove(secondBlk);
            secondBlk = (BasicBlock) secondBlk.getNext();
        }
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
