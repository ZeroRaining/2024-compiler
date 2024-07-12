package backend;

import backend.asmInstr.AsmInstr;
import backend.asmInstr.asmLS.AsmLw;
import backend.asmInstr.asmLS.AsmMove;
import backend.asmInstr.asmLS.AsmSw;
import backend.asmInstr.asmTermin.AsmCall;
import backend.itemStructure.*;
import backend.regs.*;
import com.sun.corba.se.spi.protocol.InitialServerRequestDispatcher;
import frontend.ir.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
/*todo*/ //load指令偏移量超过32位

public class RegAlloc {
    private RegAlloc() {
        // 初始化逻辑
    }

    // 单例模式下的私有静态成员变量
    private static RegAlloc instance = null;

    // 提供一个公共的静态方法，用于获取单例对象
    public static synchronized RegAlloc getInstance() {
        if (instance == null) {
            instance = new RegAlloc();
        }
        return instance;
    }
    private int FI = 0;//0代表处理Int型，1代表处理Float型
    private static int K = 5;


    private HashSet<AsmOperand> S = new HashSet<>();//暂时未用到
    private HashSet<AsmOperand> T = new HashSet<>();

    private HashMap<AsmReg, Integer> preColored = new HashMap<>();

    private HashMap<AsmOperand, HashSet<AsmOperand>> adjList = new HashMap<>();
    private HashMap<AsmOperand, AsmOperand> adjSet = new HashMap<>();
    private HashMap<AsmOperand, Integer> degree = new HashMap<>();
    private HashMap<AsmOperand, HashSet<AsmInstr>> moveList = new HashMap<>();//与该结点相关的传送指令集合

    private HashMap<AsmOperand, AsmOperand> alias = new HashMap<>();//传送指令(u,v)已被合并，v已经放到已合并结点集合，alias(v) = u

    private HashMap<AsmOperand, Integer> color = new HashMap<>();
    //@1
    private HashSet<AsmOperand> all = new HashSet<>(); // 临时寄存器集合
    private HashSet<AsmOperand> simplifyWorklist = new HashSet<>();//低度数的传送无关的节点
    private HashSet<AsmOperand> freezeWorkList = new HashSet<>();//低度数的传送有关的指令
    private HashSet<AsmOperand> spillWorkList = new HashSet<>();//高度数节点
    private HashSet<AsmOperand> spilledNodes = new HashSet<>();//本轮中要被溢出的结点集合
    private HashSet<AsmOperand> coalescedNodes = new HashSet<>();//已合并的的传送指令集合
    private HashSet<AsmOperand> coloredNodes = new HashSet<>();//已经成功着色的结点集合

    private Stack<AsmOperand> selectStack = new Stack<>();//一个包含从图中删除的临时变量的栈
    //@1 这部分，结点只能存在于其中一张表
    //@2
    private HashSet<AsmInstr> coalescedMoves = new HashSet<>();//已经合并的传送指令集合
    private HashSet<AsmInstr> constrainedMoves = new HashSet<>();//源操作数和目标操作数冲突的传送指令集合
    private HashSet<AsmInstr> frozenMoves = new HashSet<>();//不再考虑合并的传送指令
    private HashSet<AsmInstr> worklistMoves = new HashSet<>();//有可能合并的传送指令
    private HashSet<AsmInstr> activeMoves = new HashSet<>();//还未做好合并准备的传送指令集合
    //@2 传送指令集合，传送指令只能存在在其中一张表中

    public void run(AsmModule module) {
        K = 32;
        PreColor();
        for (AsmFunction function: module.getFunctions()){
            while (true) {
                initial();
                LivenessAnalysis(function);
                build(function);
                makeWorkList();
                while(!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
                !freezeWorkList.isEmpty() || !spillWorkList.isEmpty()) {
                    if (!simplifyWorklist.isEmpty()) {
                        simplify();
                    } else if (!worklistMoves.isEmpty()) {
                        Coalesce();
                    } else if (!freezeWorkList.isEmpty()) {
                        Freeze();
                    } else if (!spillWorkList.isEmpty()) {
                        SelectSpill();
                    }
                }
                AssignColors();
                if (spilledNodes.isEmpty()) {
                    for (AsmOperand vreg: color.keySet()) {
                        AsmVirReg vreg1 = (AsmVirReg) vreg;
                        vreg1.color = color.get(vreg);
                    }
                    allocRealReg(function);
                    break;
                } else {
                    RewriteProgram(function);
                }
            }
        }

    }

    private void allocRealReg(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (instrHead != null) {
                for (int i = 0; i < instrHead.regUse.size(); i++) {
                    if (instrHead.regUse.get(i) instanceof AsmVirReg) {
                        int nowColor = color.get(instrHead.regUse.get(i));
                        instrHead.changeUseReg(i,instrHead.regUse.get(i), RegGeter.AllRegsInt.get(nowColor));
                    }
                }
                for (int i = 0; i < instrHead.regDef.size(); i++) {
                    if (instrHead.regDef.get(i) instanceof AsmVirReg) {
                        int nowColor = color.get(instrHead.regDef.get(i));
                        instrHead.changeDstReg(i, instrHead.regDef.get(i), RegGeter.AllRegsInt.get(nowColor));
                    }
                }
                instrHead = (AsmInstr) instrHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }
    private void initial() {
        S.clear();
        T.clear();
        adjList.clear();
        adjSet.clear();
        degree.clear();
        moveList.clear();
        alias.clear();
        color.clear();
        all.clear();
        simplifyWorklist.clear();
        freezeWorkList.clear();
        spillWorkList.clear();
        spilledNodes.clear();
        coalescedNodes.clear();
        coloredNodes.clear();
        selectStack.clear();
        coalescedMoves.clear();
        constrainedMoves.clear();
        frozenMoves.clear();
        worklistMoves.clear();
        activeMoves.clear();
    }
    private void PreColor() {
        preColored.clear();
        preColored.put(RegGeter.ZERO, 0);
        preColored.put(RegGeter.RA, 1);
        preColored.put(RegGeter.SP, 2);
        for (int i = 0;i < 32; i++) {
            preColored.put(RegGeter.AllRegsInt.get(i),i);
        }
        for (int i = 32; i < 64; i++) {
            preColored.put(RegGeter.AllRegsFloat.get(i-32),i);
        }
    }
    private void LivenessAnalysis(AsmFunction function) {
        GetUseAndDef(function);
        GetBlockLiveInAndOut(function);
        GetLiveInterval(function);
        findLiveAtCall(function);//暂时未用到
    }
    private void GetUseAndDef(AsmFunction function) {
        all.clear();
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            blockHead.getDef().clear();
            blockHead.getUse().clear();
            AsmInstr InsHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (InsHead != null) {
                for (AsmReg one: InsHead.regUse) {
                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
                        if (!blockHead.getDef().contains(one)) {
                            blockHead.getUse().add(one);
                            if (CanBeAddToRun(one)) all.add(one);
                        }
                    }
                }
                for (AsmReg one: InsHead.regDef) {
                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
                        if (!blockHead.getDef().contains(one)) {
                            blockHead.getDef().add(one);
                            if (CanBeAddToRun(one)) all.add(one);
                        }
                    }
                }
                InsHead = (AsmInstr) InsHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }
    private void GetBlockLiveInAndOut(AsmFunction function) {
        //初始化block and instr 的LiveIn和LiveOut
        {
            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
            while (blockHead != null) {
                blockHead.LiveIn.clear();
                blockHead.LiveOut.clear();
                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                while (instrHead != null) {
                    instrHead.LiveIn.clear();
                    instrHead.LiveOut.clear();
                    instrHead = (AsmInstr) instrHead.getNext();
                }
                blockHead = (AsmBlock) blockHead.getNext();
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            AsmBlock blockTail = (AsmBlock) function.getBlocks().getTail();
            while (blockTail != null) {
                //LiveOut[B_i] <- Union (LiveIn[s]) where s belongs to succ(B_i) ;
                for (AsmBlock succBlock: blockTail.sucs) {
                    for (AsmReg one: succBlock.LiveIn){
                        blockTail.LiveOut.add(one);
                    }
                }
                //NewLiveIn <- Union (LiveUse[B_i], (LiveOut[B_i] – Def[B_i]));
                HashSet<AsmReg> NewLiveIn = new HashSet<>();
                NewLiveIn.addAll(blockTail.getUse());
                NewLiveIn.addAll(blockTail.LiveOut);
                NewLiveIn.removeAll(blockTail.getDef());

                if (!NewLiveIn.equals(blockTail.LiveIn)) {
                    changed = true;
                    blockTail.LiveIn = NewLiveIn;
                }
                blockTail = (AsmBlock) blockTail.getPrev();
            }
        }
    }

    private void GetLiveInterval(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrTail = (AsmInstr) blockHead.getInstrs().getTail();
            instrTail.LiveOut.addAll(blockHead.LiveOut);
            while (instrTail != null) {
                if (instrTail.getNext() != null) {
                    instrTail.LiveOut.addAll(((AsmInstr)instrTail.getNext()).LiveIn);
                }
                instrTail.LiveIn.addAll(instrTail.LiveOut);
                instrTail.LiveIn.removeAll(instrTail.regDef);
                instrTail = (AsmInstr) instrTail.getPrev();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void findLiveAtCall(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock)function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr)blockHead.getInstrs().getHead();
            while (instrHead != null) {
                if (instrHead instanceof AsmCall) {
                    S.addAll(instrHead.LiveOut);
                }
                instrHead = (AsmInstr) instrHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
        T.addAll(all);
        T.removeAll(S);
    }

    private void build(AsmFunction function) {
        //初始化邻接表
        for (AsmOperand one: all) {
            adjList.put(one, new HashSet<>());
            degree.put(one,0);
        }
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrTail = (AsmInstr) blockHead.getInstrs().getTail();
            HashSet<AsmOperand> live  = new HashSet<>();
            live.addAll(blockHead.LiveOut);
            while (instrTail != null) {
                if (instrTail instanceof AsmMove  && ((AsmMove) instrTail).getSrc() instanceof AsmReg && ((AsmMove) instrTail).getDst() instanceof  AsmReg) {
                    AsmMove instrMove = (AsmMove) instrTail;
                    live.removeAll(instrMove.regUse);
                    HashSet<AsmOperand> union = new HashSet<>();
                    //union.addAll(instrTail.regDef);
                    //union.addAll(instrTail.regUse);
                    for (AsmOperand D: instrMove.regDef) {
                        if (CanBeAddToRun(D) || (D instanceof AsmPhyReg && FI == 0) || (D instanceof AsmFPhyReg && FI == 1)) {
                            union.add(D);
                        }
                    }
                    for (AsmOperand U: instrMove.regUse) {
                        if (CanBeAddToRun(U) || (U instanceof AsmPhyReg && FI == 0) || (U instanceof AsmFPhyReg && FI == 1)) {
                            union.add(U);
                        }
                    }
                    if (!union.isEmpty()) {
                        for (AsmOperand n : union) {
                            if (!moveList.containsKey(n)) {
                                moveList.put(n, new HashSet<>());
                            }
                            moveList.get(n).add(instrMove);
                        }
                        worklistMoves.add(instrMove);
                    }
                }

                //live.addAll(instrTail.regDef);
                for (AsmOperand D: instrTail.regDef) {
                    if (CanBeAddToRun(D)) {
                        live.add(D);
                    }
                }
                for (AsmOperand b: instrTail.regDef) {
                    if (CanBeAddToRun(b)) {
                        for (AsmOperand l : live) {
                            AddEdge(b, l);
                        }
                    }
                }
                instrTail = (AsmInstr) instrTail.getPrev();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void makeWorkList() {
        for (AsmOperand n: all) {
            if (degree.get(n) >= K) {
                spillWorkList.add(n);
            } else if (moveList.containsKey(n) && !moveList.get(n).isEmpty()) {
                freezeWorkList.add(n);
            } else {
                simplifyWorklist.add(n);
            }
        }
    }

    private void simplify() {
        AsmOperand n = simplifyWorklist.iterator().next();
        simplifyWorklist.remove(n);
        selectStack.push(n);
        for (AsmOperand m: Adjacent(n)) {
            DecrementDegree(m);
        }
    }

    private void Coalesce() {
        AsmInstr m = worklistMoves.iterator().next();
        AsmOperand x = GetAlias(((AsmMove)m).getDst());
        AsmOperand y = GetAlias(((AsmMove)m).getSrc());
        AsmOperand u;
        AsmOperand v;
        if (preColored.containsKey(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(m);
        if (u == v) {
            coalescedMoves.add(m);
            AddWorkList(u);
        }else if (preColored.containsKey(v) || (adjSet.containsKey(u) && adjSet.get(u) == v)) {//uv冲突或者uv都是预着色点
            constrainedMoves.add(m);
            AddWorkList(u);
            AddWorkList(v);
        }else {
            boolean Ok = true;
            for (AsmOperand t : Adjacent(v)) {
                if (!OK(t, u)) {
                    Ok = false;
                    break;
                }
            }
            HashSet<AsmOperand> union = new HashSet<>();
            union.addAll(Adjacent(u));
            union.addAll(Adjacent(v));
            if ((preColored.containsKey(u) && Ok) || (!preColored.containsKey(u) && Conservative(union))) {
                coalescedMoves.add(m);
                Combine(u, v);
                AddWorkList(u);
            } else {
                activeMoves.add(m);
            }
        }
    }

    private void Combine(AsmOperand u, AsmOperand v) {
        if (freezeWorkList.contains(v)) {
            freezeWorkList.remove(v);
        } else {
            spillWorkList.remove(v);
        }
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList.get(u).addAll(moveList.get(v));
        HashSet<AsmOperand> V = new HashSet<>();
        V.add(v);
        EnableMoves(V);
        for (AsmOperand t: Adjacent(v)) {
            AddEdge(t,u);
            DecrementDegree(t);
        }
        if ((degree.containsKey(u) && degree.get(u) >= K) && freezeWorkList.contains(u)) {
            freezeWorkList.remove(u);
            spillWorkList.add(u);
        }
    }

    private void Freeze() {
        AsmOperand u = freezeWorkList.iterator().next();
        freezeWorkList.remove(u);
        simplifyWorklist.add(u);

    }
    private void FreezeMoves(AsmOperand u) {
        for (AsmInstr m: NodeMoves(u)) {
            AsmOperand x = ((AsmMove)m).getDst();
            AsmOperand y = ((AsmMove)m).getSrc();
            AsmOperand v;
            if (GetAlias(y) == GetAlias(u)) {
                v = GetAlias(x);
            } else {
                v = GetAlias(y);
            }
            activeMoves.remove(m);
            frozenMoves.add(m);
            if (NodeMoves(v).isEmpty() && degree.get(v) < K) {
                freezeWorkList.remove(v);
                simplifyWorklist.add(v);
            }
        }
    }

    private void SelectSpill() {
        AsmOperand m = spillWorkList.iterator().next();//目前是随机选，后面再换/todo/
        spillWorkList.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);

    }

    private void AssignColors() {
        while (!selectStack.isEmpty()) {
           AsmOperand n = selectStack.pop();
            HashSet<Integer> okColors = new HashSet<>();
            for (int k = 0;k<K;k++) {
                if (k >= 5)
                okColors.add(k);
            }
            for (AsmOperand w: adjList.get(n)) {
                AsmOperand Gw = GetAlias(w);
                if (coloredNodes.contains(Gw) || preColored.containsKey(Gw)){
                    if (coloredNodes.contains(Gw)) {
                        okColors.remove(color.get(Gw));
                    } else {
                        okColors.remove(preColored.get(Gw));
                    }
                }
            }
            if (okColors.isEmpty()) {
                spilledNodes.add(n);
            } else {
                coloredNodes.add(n);
                int c = okColors.iterator().next();
                color.put(n, c);
            }

        }
        for (AsmOperand n: coalescedNodes) {
            if (color.containsKey(GetAlias(n))) {
                color.put(n, color.get(GetAlias(n)));
            } else {
                AsmOperand n2 = GetAlias(n);
                int preColor = preColored.get(n2);
                color.put(n, preColor);
            }
        }
    }

    private void RewriteProgram(AsmFunction function){
        HashSet<AsmOperand> newTemps = new HashSet<>();
        for (AsmOperand v: spilledNodes) {
            int spillPlace = function.getAllocaSize();
            function.addAllocaSize(4);
            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
            while (blockHead != null) {
                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                while (instrHead != null) {
                    //为regUse进行spill处理
                    if (instrHead.regUse.contains(v)) {
                        AsmVirReg v1 = new AsmVirReg();
                        for (int i = 0; i< instrHead.regUse.size();i++) {
                            if (instrHead.regUse.get(i) == v) {
                                instrHead.changeUseReg(i,instrHead.regUse.get(i), v1);
                            }
                        }
                        AsmImm12 place = new AsmImm12(spillPlace);
                        AsmLw load = new AsmLw(v1, RegGeter.SP,place);
                        instrHead.insertBefore(load);
                        newTemps.add(v1);
                    }
                    if (instrHead.regDef.contains(v)) {
                        AsmVirReg v2 = new AsmVirReg();
                        for (int i = 0; i < instrHead.regDef.size(); i++) {
                            if (instrHead.regDef.get(i) == v) {
                                instrHead.changeDstReg(i,instrHead.regUse.get(i), v2);
                            }
                        }
                        AsmImm12 place = new AsmImm12(spillPlace);
                        AsmSw store = new AsmSw(v2, RegGeter.SP, place);
                        instrHead.insertAfter(store);
                        newTemps.add(v2);
                    }
                    instrHead = (AsmInstr) instrHead.getNext();

                }
                blockHead= (AsmBlock) blockHead.getNext();
            }
        }
    }
    private boolean Conservative(HashSet<AsmOperand> nodes) {
        int k = 0;
        for (AsmOperand n: nodes) {
            if (degree.get(n) >= K) {
                k = k + 1;
            }
        }
        return k < K;
    }

    private boolean OK(AsmOperand t, AsmOperand r) {
        if ((degree.containsKey(t) && degree.get(t) < K )|| preColored.containsKey(t) || (adjSet.containsKey(t) && adjSet.get(t).equals(r))) {
            return true;
        } else {
            return false;
        }
    }
    private void AddWorkList(AsmOperand u) {
        if (!preColored.containsKey(u) && !MoveRelated(u)&& degree.get(u) < K) {
            freezeWorkList.remove(u);
            simplifyWorklist.add(u);
        }
    }
    private AsmOperand GetAlias(AsmOperand n) {//递归寻找被结合的点
        if (coalescedNodes.contains(n)) {
           return GetAlias(alias.get(n));
        }
        return n;
    }
    private void DecrementDegree(AsmOperand m) {
        if (!degree.containsKey(m)) {
            return;
        }
        int d = degree.get(m);
        degree.put(m, d-1);
        if (d == K) {
            HashSet<AsmOperand> union = new HashSet<>();
            union.addAll(Adjacent(m));
            union.add(m);
            EnableMoves(union);
            spillWorkList.remove(m);
            if (MoveRelated(m)) {
                freezeWorkList.add(m);
            } else {
                simplifyWorklist.add(m);
            }

        }
    }
    private boolean MoveRelated(AsmOperand m) {
        return !NodeMoves(m).isEmpty();
    }

    private void EnableMoves(HashSet<AsmOperand> nodes) {
        for (AsmOperand n: nodes) {
            for (AsmInstr m: NodeMoves(n)) {
                if (activeMoves.contains(m)) {
                    activeMoves.remove(m);
                    worklistMoves.add(m);
                }
            }
        }
    }

    public HashSet<AsmOperand> Adjacent(AsmOperand n) {
        HashSet<AsmOperand> result = new HashSet<>();
        if (adjList.containsKey(n)) {
            result.addAll(adjList.get(n));
            result.removeAll(selectStack);
            result.removeAll(coalescedNodes);
        }
        return result;
    }
    public HashSet<AsmInstr> NodeMoves(AsmOperand n) { //与操作数相关的传送指令集合（未被冻结
        HashSet<AsmInstr> result = new HashSet<>();
        result.addAll(moveList.get(n));
        result.removeAll(activeMoves);
        result.removeAll(worklistMoves);
        return result;
    }

    private void AddEdge(AsmOperand b, AsmOperand l){ //未按照书中所给实现
        if( !(adjSet.containsKey(b) && adjSet.get(b) == l) && b != l) {
            adjSet.put(b,l);
            adjSet.put(l,b);
        }
        if (!preColored.containsKey(b)) {
            adjList.get(b).add(l);
            degree.put(b,degree.get(b) + 1);
        }
        if (!preColored.containsKey(l)) {
            adjList.get(l).add(b);
            degree.put(l,degree.get(l) + 1);
        }
    }

    private boolean CanBeAddToRun(AsmOperand m) {
       if ((m instanceof AsmVirReg && FI == 0) || (m instanceof AsmFVirReg && FI == 1)){
            return true;
       }
//       else if ((m instanceof AsmPhyReg && FI == 0) || (m instanceof AsmFPhyReg && FI == 1)) {
//           return true;
//       }
       else {
           return false;
       }
    }

    private static boolean debug = false;
    private void logout(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
}
