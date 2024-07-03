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
        }
    }
    //b1 -> b2 -> b3
    private void makeIDoms(Function function) {
        for (CustomList.Node item : function.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) item;
            for (BasicBlock dom1 : block.getDoms()) {
                for (BasicBlock dom2 : dom1.getDoms()) {
                    if (!block.getDoms().contains(dom2)) {
                        block.getIDoms().add(dom2);
                    }
                }
            }
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
            for (CustomList.Node node : function.getBasicBlocks()) {
                BasicBlock otherBlk = (BasicBlock) node;
                if (!independent.contains(otherBlk)) {
                    block.getDoms().add(otherBlk);
                }
            }
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
        while (blks.hasNext()) {
            BasicBlock block = (BasicBlock) blks.next();
            Use use = block.getBeginUse();
            if (use == null) {
                blks.remove();
                continue;
            }
            for (;use != block.getEndUse(); use = (Use) use.getNext()) {
                BasicBlock user = use.getUser().getParentBB();
                block.getPres().add(user);
                user.getSucs().add(block);
            }
        }
    }



}
