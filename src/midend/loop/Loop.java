package midend.loop;

import frontend.ir.Value;
import frontend.ir.instr.otherop.cmp.CmpCond;
import frontend.ir.structure.BasicBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Loop {
    private BasicBlock entering;
    private BasicBlock preCond;
    private BasicBlock header;
    private HashSet<BasicBlock> exits = new HashSet<>();
    private HashSet<BasicBlock> exitings = new HashSet<>();
    private HashSet<BasicBlock> latchs = new HashSet<>();
    private ArrayList<BasicBlock> blks = new ArrayList<>();
    private ArrayList<Loop> innerLoops = new ArrayList<>();
    private ArrayList<BasicBlock> sameLoopDepth = new ArrayList<>();
    private Loop prtLoop;

    private Value var;
    private Value begin;
    private Value end;
    private Value step;
    private CmpCond cmpCond;

    public Loop(BasicBlock header) {
        this.header = header;
        this.addBlk(header);
        prtLoop = null;
    }

    public void setPreCond(BasicBlock preCond) {
        this.preCond = preCond;
    }

    public BasicBlock getPreCond() {
        return preCond;
    }

    public void setEntering(BasicBlock entering) {
        this.entering = entering;
    }

    public BasicBlock getEntering() {
        return entering;
    }

    public void setSameLoopDepth(ArrayList<BasicBlock> sameLoopDepth) {
        this.sameLoopDepth = sameLoopDepth;
    }

    public ArrayList<BasicBlock> getSameLoopDepth() {
        return sameLoopDepth;
    }

    public BasicBlock getHeader() {
        return header;
    }

    public Loop getPrtLoop() {
        return prtLoop;
    }

    public void setPrtLoop(Loop prtLoop) {
        this.prtLoop = prtLoop;
    }

    public void addLoop(Loop inner) {
        if (innerLoops.contains(inner)) return;
        innerLoops.add(inner);
    }

    public void addBlk(BasicBlock blk) {
        //todo:为什么一个块会被加好几次？
        if (blks.contains(blk)) return;
        blks.add(blk);
    }

    public void addExitingBlk(BasicBlock blk) {
        if (exitings.contains(blk)) return;
        exitings.add(blk);
    }

    public void addLatchBlk(BasicBlock blk) {
        if (latchs.contains(blk)) return;
        latchs.add(blk);
    }

    public void addExitBlk(BasicBlock blk) {
        if (exits.contains(blk)) return;
        exits.add(blk);
    }

    public HashSet<BasicBlock> getExits() {
        return exits;
    }

    public ArrayList<Loop> getInnerLoops() {
        return innerLoops;
    }

    public ArrayList<BasicBlock> getBlks() {
        return blks;
    }

    public void reverse() {
        blks.remove(header);
        Collections.reverse(blks);
        blks.add(0, header);
        Collections.reverse(innerLoops);
    }
}
