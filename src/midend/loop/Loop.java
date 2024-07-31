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
    private HashSet<BasicBlock> exit = new HashSet<>();
    private HashSet<BasicBlock> exiting = new HashSet<>();
    private HashSet<BasicBlock> latch = new HashSet<>();
    private ArrayList<BasicBlock> blks = new ArrayList<>();
    private ArrayList<Loop> inner = new ArrayList<>();
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
        inner.add(subLoop);
    }

    public void addBlk(BasicBlock blk) {
        blks.add(blk);
    }

    public void addExitingBlk(BasicBlock blk) {
        exiting.add(blk);
    }

    public void addLatchBlk(BasicBlock blk) {
        latch.add(blk);
    }

    public void addExitblk(BasicBlock blk) {
        exit.add(blk);
    }

    public HashSet<BasicBlock> getExit() {
        return exit;
    }

    public ArrayList<Loop> getInner() {
        return inner;
    }

    public ArrayList<BasicBlock> getBlks() {
        return blks;
    }

    public void reverse() {
        blks.remove(header);
        Collections.reverse(blks);
        blks.add(0, header);
        Collections.reverse(inner);
    }
}
