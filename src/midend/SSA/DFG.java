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
    private Queue<Integer> worklist;
    private HashMap<Integer, Boolean> visited;
    private HashMap<Integer, Boolean> placed;
    private ArrayList<Function> functions;
    public DFG(ArrayList<Function> functions) {
        worklist = new LinkedList<Integer>();
        visited = new HashMap<>();
        placed = new HashMap<>();
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
                for (BasicBlock dom2 : dom1.getDoms()) {
                    if (!block.getDoms().contains(dom2)) {
                        iDoms.add(dom2);
                    }
                }
            }
            block.setIDoms(iDoms);
        }
    }

    private void makeDoms(Function function) {
        for (CustomList.Node item : function.getBasicBlocks()) {
            HashSet<BasicBlock> independent = new HashSet<>();
            BasicBlock block = (BasicBlock) item;
            for (CustomList.Node node : function.getBasicBlocks()) {
                BasicBlock otherBlk = (BasicBlock) node;
                dfs4dom(block, otherBlk, independent);
            }
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
        if (block == otherBlk) {
            return;
        }
        if (independent.contains(otherBlk)) {
            return;
        }
        independent.add(otherBlk);
        for (BasicBlock block1 : block.getSucs()) {
            dfs4dom(block, block1, independent);
        }
    }



    private void removeBlk(Function function) {
        Iterator<CustomList.Node> blks = function.getBasicBlocks().iterator();
        HashMap<BasicBlock, HashSet<BasicBlock>> pres = new HashMap<>();
        HashMap<BasicBlock, HashSet<BasicBlock>> sucs = new HashMap<>();

        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            if (block.getBeginUse() == null) {
                blks.remove();
            } else {
                pres.put(block, new HashSet<>());
                sucs.put(block, new HashSet<>());
            }
        }

        blks = function.getBasicBlocks().iterator();
        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            Use use = block.getBeginUse();
            for (;use != block.getEndUse(); use = (Use) use.getNext()) {
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
