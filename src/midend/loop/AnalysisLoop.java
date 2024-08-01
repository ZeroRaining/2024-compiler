package midend.loop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import midend.SSA.DFG;

import java.util.*;

public class AnalysisLoop {
    private static HashMap<BasicBlock, Loop> header2loop = new HashMap<>();
    private static ArrayList<Loop> outLoop = new ArrayList<>();
    private static ArrayList<Loop> allLoop = new ArrayList<>();
    public static void execute(ArrayList<Function> functions) {
        for (Function function : functions) {
            init();
            findAllLoop(function);
            fillInLoop();
            finalized(function);
        }
    }
    private static void finalized(Function function){
        function.setAllLoop(allLoop);
        function.setHeader2loop(header2loop);
        function.setOuterLoop(outLoop);
    }
    private static void init() {
        header2loop = new HashMap<>();
        outLoop = new ArrayList<>();
        allLoop = new ArrayList<>();
    }
    private static void fillInLoop() {
        for (Loop loop : allLoop) {
            for (BasicBlock blk : loop.getBlks()) {
                for (BasicBlock suc : blk.getSucs()) {
                    if (!loop.getBlks().contains(suc)) {
                        loop.addExitingBlk(blk);
//                        if (loop.getExit() == suc || loop.getExit() == null) {
                            loop.addExitblk(suc);
//                        } else {
//                            throw new RuntimeException("竟然会有两个结束块");
//                        }
                    }
                }
            }
            for (BasicBlock pre : loop.getHeader().getPres()) {
                if (loop.getBlks().contains(pre)) {
                    loop.addLatchBlk(pre);
                }
            }
        }
        for (Loop loop : allLoop) {
            ArrayList<BasicBlock> sameDepth = new ArrayList<>(loop.getBlks());
            for (Loop in : loop.getInnerLoops()) {
                sameDepth.removeAll(in.getBlks());
            }
            loop.setSameLoopDepth(sameDepth);
        }
    }

    private static void findAllLoop(Function function) {
        ArrayList<BasicBlock> order = DFG.getDomPostOrder(function);
//        System.out.println(order);
//        for (BasicBlock block : order) {
//            System.out.println(block);
//            System.out.println(block.getDoms());
//        }
        // 如果blk支配其前驱pre，则构成循环blk1 -> blk2 -> blk1，blk2->blk1为backEdge
        for (BasicBlock blk : order) {
            Stack<BasicBlock> backEdges = new Stack<>();
            for (BasicBlock pre : blk.getPres()) {
                if (blk.getDoms().contains(pre)) {
//                    System.out.println(blk + " doms " + pre);
                    backEdges.push(pre);
                }
            }
            //有backEdge说明有循环，且循环头为blk
            if (!backEdges.isEmpty()) {
                //第一个循环
                Loop loop = new Loop(blk);
                makeLoop(loop, backEdges);
            }
        }
        populateLoops(order);
    }

    private static void populateLoops(ArrayList<BasicBlock> order) {
        for (BasicBlock blk : order) {
            //insert
            Loop innerLoop = header2loop.get(blk);
            if (innerLoop != null && innerLoop.getHeader().equals(blk)) {
                if (innerLoop.getPrtLoop() != null) {
                    innerLoop.getPrtLoop().addLoop(innerLoop);
                } else {
                    outLoop.add(innerLoop);
                }
                innerLoop.reverse();
                innerLoop = innerLoop.getPrtLoop();
            }
            while (innerLoop != null) {
                innerLoop.addBlk(blk);
                innerLoop = innerLoop.getPrtLoop();
            }
        }
        allLoop.clear();
        Stack<Loop> stack = new Stack<>();
        stack.addAll(outLoop);
        allLoop.addAll(outLoop);
        //顺序？
        while (!stack.isEmpty()) {
            Loop loop = stack.pop();
            if (!loop.getInnerLoops().isEmpty()) {
                stack.addAll(loop.getInnerLoops());
                allLoop.addAll(loop.getInnerLoops());
            }
        }
    }

    /*
     * 在discoverAndMapSubloop中，从所有backedge的source开始，沿着reverse cfg找到所有的循环体block。
     * 注意因为analyze中是按支配树的后序进行遍历的，因此任何存在的sub-loop都在当前循环头的支配域中，并且根据
     * 后序遍历的性质他们肯定已经被遍历完毕，配合循环性质：两个循环要么包含要么不相交，绝不可能部分重合，可以证
     * 明这个算法的正确性。
     * 在遍历的过程中会分为两种情况，一种是候选block目前不在任何循环中，则直接将该block放到当前循环中（函数的
     * 第一个参数）并将其所有前继成为候选block，另一种是候选block已经是某个循环的一部分，则这时候将这个循环的
     * outermost循环成为当前循环的父循环，并将其循环头的所有前继成为候选block。
     */
    private static void makeLoop(Loop loop, Stack<BasicBlock> backEdges) {
        while (!backEdges.isEmpty()) {
            BasicBlock header = backEdges.pop();
            Loop innerLoop = header2loop.get(header);
            //由于后续遍历，最上面的，一定是最外的循环
            if (innerLoop == null) {
                header2loop.put(header, loop);
                if (header.equals(loop.getHeader())) {
                    continue;
                }
                /*  如果循环头不是当前的
                 *  则当前循环为某一循环的子循环
                 *  把当前头的前面所有的压栈
                 */
                for (BasicBlock pre : header.getPres()) {
                    backEdges.push(pre);
                }

            } else {
                while (innerLoop.getPrtLoop() != null) {
                    innerLoop = innerLoop.getPrtLoop();
                }
                if (innerLoop == loop) {
                    continue;
                }
                innerLoop.setPrtLoop(loop);
                for (BasicBlock pre : innerLoop.getHeader().getPres()) {
                    if (header2loop.get(pre) == null || !header2loop.get(pre).equals(innerLoop)) {
                        backEdges.push(pre);
                    }
                }
            }
        }
    }
}
