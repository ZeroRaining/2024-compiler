package midend.loop;

import Utils.CustomList;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.binop.BinaryOperation;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.otherop.cmp.Cmp;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
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
            execute4func(function);
        }
    }

    public static void execute4func(Function function) {
        init(function);
        findAllLoop(function);
        fillInLoop();
        finalized(function);
    }

    private static void dfs4loopDom(BasicBlock block, BasicBlock otherBlk, HashSet<BasicBlock> linked, ArrayList<BasicBlock> loopBlks) {
        if (block.equals(otherBlk)) {
            return;
        }
        if (linked.contains(otherBlk)) {
            return;
        }
        linked.add(block);
        for (BasicBlock next : block.getSucs()) {
            if (!loopBlks.contains(next)) {
                if (!linked.contains(next) && next != otherBlk){
                    dfs4loopDom(next, otherBlk, linked, loopBlks);
                }
            }
        }
    }

    public static void dom4loop(Loop loop) {
        BasicBlock firstBlk = loop.getBlks().get(0);
        for (BasicBlock block : loop.getBlks()) {
            HashSet<BasicBlock> linked = new HashSet<>();
            dfs4loopDom(firstBlk, block, linked, loop.getBlks());

            HashSet<BasicBlock> doms = new HashSet<>();
            for (BasicBlock otherBlk : loop.getBlks()) {
                if (!linked.contains(otherBlk)) {
                    doms.add(otherBlk);
                }
            }
            block.setLoopDoms(doms);
        }
    }

    private static void finalized(Function function){
        function.setAllLoop(allLoop);
        function.setHeader2loop(header2loop);
        function.setOuterLoop(outLoop);
        for (Loop loop : outLoop) {
            dfs4color(loop, 1);
        }
    }

    private static void dfs4color(Loop loop, int depth) {
        loop.colorBlk(depth);
        for (Loop inner : loop.getInnerLoops()) {
            dfs4color(inner, depth + 1);
        }
    }

    private static void init(Function function) {
        header2loop = new HashMap<>();
        outLoop = new ArrayList<>();
        allLoop = new ArrayList<>();
        CustomList.Node block =  function.getBasicBlocks().getHead();
        while (block != null) {
            ((BasicBlock) block).setBlockType(BlockType.OUTOFLOOP);
            ((BasicBlock) block).setLoopDepth(0);
            block =  block.getNext();
        }
    }
    private static void fillInLoop() {
        for (Loop loop : allLoop) {
            for (BasicBlock blk : loop.getBlks()) {
                for (BasicBlock suc : blk.getSucs()) {
                    if (!loop.getBlks().contains(suc)) {
                        loop.addExitingBlk(blk);
                        loop.addExitBlk(suc);
                    }
                }
            }
            for (BasicBlock pre : loop.getHeader().getPres()) {
                if (loop.getBlks().contains(pre)) {
                    loop.addLatchBlk(pre);
                }
            }
//            System.out.println(loop.getHeader() + " " + loop.getLatchs());
        }
        for (Loop loop : allLoop) {
            for (BasicBlock blk : loop.getHeader().getPres()) {
                if (loop.getBlks().contains(blk)) continue;
                loop.setPreHeader(blk);
                blk.setEntering(true);
//                loop.setPreCond(blk);
            }
            ArrayList<BasicBlock> sameDepth = new ArrayList<>(loop.getBlks());
            for (Loop in : loop.getInnerLoops()) {
                sameDepth.removeAll(in.getBlks());
            }
            loop.setSameLoopDepth(sameDepth);
//            System.out.println(loop.getHeader() + " entering " + loop.getEntering() + " " + loop.getSameLoopDepth());
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

    public static void LoopIndVars(Function function){
        for(Loop loop : function.getAllLoop()){
            SingleIndVars(loop);
        }
    }

    private static void SingleIndVars(Loop loop) {
        if (!loop.isSimpleLoop()) {
            return;
        }
        //TODO：短路求值？？
//        loop.LoopPrint();

        /*blk_0: anon: entering
            br label %blk_2
        blk_2: anon: header loopCondition exiting
            %phi_0 = phi i32 [ 1, %blk_0 ], [ %reg_6, %blk_3 ]
            %reg_3 = call i32 @getint()
            %reg_4 = icmp slt i32 %phi_0, %reg_3 anon: cond
            br i1 %reg_4, label %blk_3, label %blk_1 anon: br
        blk_3: anon: latch
            %reg_6 = add i32 %phi_0, 1
            br label %blk_2
        blk_1: anon: exit
            %phi_2 = phi i32 [ %phi_0, %blk_2 ]
            ret i32 %phi_2*/

        /*
        define i32 @main() {
        blk_0:
            br label %blk_2
        blk_2:
            %reg_4 = icmp slt i32 0, 20
            br i1 %reg_4, label %blk_5, label %blk_4
        blk_5:
            br label %blk_3 anon: pre-header
        blk_3:
            %phi_1 = phi i32 [ 0, %blk_5 ], [ %reg_8, %blk_7 ] anon: header & body
            %reg_8 = add i32 %phi_1, 3
            %reg_10 = sub i32 %reg_10, 2
            br label %blk_7
        blk_7: anon: latch & exiting
            %reg_6 = icmp slt i32 %reg_8, 20 anon: cond
            br i1 %reg_6, label %blk_3, label %blk_6 anon: br
        blk_6:
            br label %blk_4 anon: exit
        blk_4:
            %phi_0 = phi i32 [ %reg_8, %blk_6 ], [ 0, %blk_2 ]
            br label %blk_1
        blk_1:
            ret i32 0
        }
        */
        BasicBlock header = loop.getHeader();
        BasicBlock latch = loop.getLatchs().get(0);

        PhiInstr itVar;
        Value itEnd;
        if (latch.getEndInstr() instanceof JumpInstr) {
            throw new RuntimeException(loop + " instr: " + header.getEndInstr().print());
        }
        BranchInstr br = (BranchInstr) latch.getEndInstr();
        Value cond = br.getCond();

        if (cond instanceof ConstValue) {
            return;
        }
        if (!(cond instanceof Cmp)) {
            throw new RuntimeException("是不是你写的有问题？");
        }
        Value itAlu;

        Value op1 = ((Cmp) cond).getOp1();
        Value op2 = ((Cmp) cond).getOp2();
        //TODO：先常数传播
        if (op1 instanceof ConstValue) {
            itAlu = op2;
            itEnd = op1;
        } else if (op2 instanceof ConstValue) {
            itAlu = op1;
            itEnd = op2;
        } else {
            return;
        }
        if (!(itAlu instanceof BinaryOperation)) {
            return;
        }
        op1 = ((BinaryOperation) itAlu).getOp1();
        op2 = ((BinaryOperation) itAlu).getOp2();
        Value itStep;
        if (op1 instanceof PhiInstr) {
            itVar = (PhiInstr) op1;
            itStep = op2;
        } else if (op2 instanceof PhiInstr) {
            itVar = (PhiInstr) op2;
            itStep = op1;
        } else {
            return;
        }
        if (!(itStep instanceof ConstValue)) {
            return;
        }
        BasicBlock preHeader = loop.getPreHeader();//TODO:fix preHeader & preCond
        Value itInit = itVar.getValues().get(itVar.getPrtBlks().indexOf(preHeader));

//        if (op1 instanceof PhiInstr) {
//            itVar = (PhiInstr) op1;
//            itEnd = op2;
//        } else if (op2 instanceof PhiInstr) {
//            itVar = (PhiInstr) op2;
//            itEnd = op1;
//        } else {
//            return;
//            /*部分情况下可能没有一个phi作为循环变量，这种的可以直接被优化掉
//            *
//            * TODO：
//            *  int main() {
//            *     int a= 1;
//            *     while (a < 100) {
//            *         a = a + 1;
//            *     }
//            *     return a;
//            *  }
//            *  这种要被循环不变量后提干掉！！
//            *
//            * */
//            //throw new RuntimeException("op1: " + op1 + " op2: " + op2);
//        }

        {
        /*blk_0: anon: entering
            br label %blk_2
        blk_2: anon: header loopCondition exiting
            %phi_0 = phi i32 [ 1, %blk_0 ], [ %reg_6, %blk_3 ] anon: op1
            %reg_3 = call i32 @getint() anon: op2
            %reg_4 = icmp slt i32 %phi_0, %reg_3 anon: cond
            br i1 %reg_4, label %blk_3, label %blk_1 anon: br
        blk_3: anon: latch
            %reg_6 = add i32 %phi_0, 1
            br label %blk_2
        blk_1: anon: exit
            %phi_2 = phi i32 [ %phi_0, %blk_2 ]
            ret i32 %phi_2*/
        }

//        Value itAlu;
//        Value itInit;
//        if (itVar.getPrtBlks().size() > 2) {
//            return;
//        }
//        BasicBlock latch = loop.getLatchs().get(0);
//        int index = itVar.getPrtBlks().indexOf(latch);
//        if (index == 1) {
//            itAlu = itVar.getValues().get(1);
//            itInit = itVar.getValues().get(0);
//        } else if (index == 0) {
//            itAlu = itVar.getValues().get(0);
//            itInit = itVar.getValues().get(1);
//        } else {
//            return;
//        }
//
//        Value itStep;
//        if (!(itAlu instanceof BinaryOperation)) {
//            return;
////            throw new RuntimeException(itAlu.toString());
//        }
//        if (((BinaryOperation) itAlu).getOp1() == itVar) {
//            itStep = ((BinaryOperation) itAlu).getOp2();
//        } else if (((BinaryOperation) itAlu).getOp2() == itVar) {
//            itStep = ((BinaryOperation) itAlu).getOp1();
//        } else {
//            return;
//        }

        String opName = ((BinaryOperation) itAlu).getOperationName();
        if (opName.contains("add") || opName.contains("sub") || opName.contains("mul")) {
            loop.setIndVar(itVar, itInit, itEnd, itAlu, itStep, cond);
        }
        loop.LoopPrint();
    }

}
