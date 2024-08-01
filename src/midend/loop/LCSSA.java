package midend.loop;

import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LCSSA {
    private static int phiCnt;
    public static void execute(ArrayList<Function> functions) {
        for (Function function : functions) {
            phiCnt = function.getPhiIndex();
            addPhi(function);
            function.setCurPhiIndex(phiCnt);
        }
    }

    private static void addPhi(Function function) {
        for (Loop loop : function.getOuterLoop()) {
            dfs4addPhi(loop);
        }
    }

    private static void dfs4addPhi(Loop loop) {
        for (Loop innerLoop : loop.getInnerLoops()) {
            dfs4addPhi(innerLoop);
        }

        HashSet<BasicBlock> exits = loop.getExits();
        assert !exits.isEmpty();
        for (BasicBlock blk : loop.getBlks()) {
            Instruction instr = (Instruction) blk.getInstructions().getHead();
            while (instr != null) {
                //从外界流入的值，在结束快就会出现至少两个来源 => phi
                if (liveOutLoop(instr, loop)) {
                    //在每一个结束块加入phi
                    HashMap<BasicBlock, PhiInstr> exit2phi = new HashMap<>();
                    BasicBlock prt = instr.getParentBB();
                    for (BasicBlock exitBlk : loop.getExits()) {
                        //如果当前的指令的父块支配结束块，则会产生一个phi
                        if (!exit2phi.containsKey(exitBlk) && prt.getDoms().contains(exitBlk)) {
                            ArrayList<Value> phiValues = new ArrayList<>();
                            ArrayList<BasicBlock> prts = new ArrayList<>();
                            //phi的值由前驱决定
                            //将phi替换为后面使用他的
                            for (BasicBlock pre : exitBlk.getPres()) {
                                phiValues.add(instr);
                                prts.add(pre);
                            }
                            PhiInstr phi = new PhiInstr(phiCnt++, instr.getDataType(), phiValues, prts);
                            System.out.println(phi.print() + " " + phiCnt);
                            exitBlk.addInstrToHead(phi);
                            exit2phi.put(exitBlk, phi);
                        }
                    }
                    //rename
                    Use use = instr.getBeginUse();
                    while (use != null) {
                        Instruction user = use.getUser();
                        BasicBlock userBlk;
                        //phi中的prt与value的parentBB不一定相同！！！！
                        if (user instanceof PhiInstr) {
                            userBlk = ((PhiInstr) user).getPrtBlks().get(((PhiInstr) user).getValues().indexOf(instr));
                        } else {
                            userBlk = user.getParentBB();
                        }
                        if(userBlk == prt || loop.getBlks().contains(userBlk)){
                            use = (Use) use.getNext();
                            continue;
                        }

                        PhiInstr phi = getPhiValue(exit2phi, userBlk, loop);

                        user.modifyUse(instr, phi);
                        use = (Use) use.getNext();
                    }
                }
                instr = (Instruction) instr.getNext();
            }
        }
    }

    private static PhiInstr getPhiValue(HashMap<BasicBlock, PhiInstr> exit2phi, BasicBlock userBlk, Loop loop) {
        PhiInstr phi = exit2phi.get(userBlk);
        if (phi != null) {
            return phi;
        }
        BasicBlock iDomor = userBlk.getiDomor();
        if (!loop.getBlks().contains(iDomor)) {
            phi = getPhiValue(exit2phi, iDomor, loop);
            exit2phi.put(userBlk, phi);
            return phi;
        }
        ArrayList<Value> values = new ArrayList<>();
        ArrayList<BasicBlock> prtBlks = new ArrayList<>();
        for (BasicBlock pre : userBlk.getPres()) {
            values.add(getPhiValue(exit2phi, pre, loop));
            prtBlks.add(pre);
        }
        phi = new PhiInstr(phiCnt++, values.get(0).getDataType(), values, prtBlks);
        System.out.println(phi.print() + " " + phiCnt);
        userBlk.addInstrToHead(phi);
        exit2phi.put(userBlk, phi);
        return phi;
    }

    private static boolean liveOutLoop(Instruction instr, Loop loop) {
        Use use = instr.getBeginUse();
        while (use != null) {
            if (!loop.getBlks().contains(use.getUser().getParentBB())) {
                return true;
            }
            use = (Use) use.getNext();
        }
        return false;
    }
}
