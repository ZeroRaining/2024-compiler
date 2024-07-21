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
    public static void doDFG(HashSet<Function> functions) {
        for (Function function : functions) {
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
                boolean flag = block.getDoms().contains(dom1) && block != dom1;
                if (flag) {
                    for (BasicBlock temp: block.getDoms()) {
                        if (!temp.equals(block) && !temp.equals(dom1)) {
                            if (temp.getDoms().contains(dom1)) {
                                flag = false;
                                break;
                            }
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
            HashSet<BasicBlock> linked = new HashSet<>();
            dfs4dom(firstBlk, block, linked);

            HashSet<BasicBlock> doms = new HashSet<>();
            for (CustomList.Node node : function.getBasicBlocks()) {
                BasicBlock otherBlk = (BasicBlock) node;
                if (!linked.contains(otherBlk)) {
                    doms.add(otherBlk);
                }
            }
            block.setDoms(doms);
        }
    }

    private static void dfs4dom(BasicBlock block, BasicBlock otherBlk, HashSet<BasicBlock> linked) {
        if (block.equals(otherBlk)) {
            return;
        }
        if (linked.contains(otherBlk)) {
            return;
        }
        linked.add(block);
        for (BasicBlock next : block.getSucs()) {
            if (!linked.contains(next) && next != otherBlk){
                dfs4dom(next, otherBlk, linked);
            }
        }
    }

}
