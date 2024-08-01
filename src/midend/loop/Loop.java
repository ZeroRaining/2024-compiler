package midend.loop;

import frontend.ir.Value;
import frontend.ir.instr.otherop.cmp.CmpCond;
import frontend.ir.structure.BasicBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Loop {
    private BasicBlock entering;
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

    public void addLoop(Loop subLoop) {
        innerLoops.add(subLoop);
    }

    public void addBlk(BasicBlock blk) {
        blks.add(blk);
    }

    public void addExitingBlk(BasicBlock blk) {
        exitings.add(blk);
    }

    public void addLatchBlk(BasicBlock blk) {
        latchs.add(blk);
    }

    public void addExitblk(BasicBlock blk) {
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
