package backend.itemStructure;

import Utils.CustomList;
import backend.regs.AsmReg;

import java.util.HashSet;

public class AsmFunction {
    private CustomList blocks = new CustomList();
    private HashSet<AsmReg> usedVirRegs = new HashSet<>();
    private int allocaSize = 0;
    private int raSize = 0;
    private int argsSize = 0;
    boolean isTail = false;

    public void addBlock(AsmBlock block) {
        blocks.addToTail(block);
    }

    public void setArgsSize(int argsSize) {
        this.argsSize = Math.max(argsSize, this.argsSize);
    }

    public void setRaSize(int raSize) {
        this.raSize = raSize;
    }

    public void setIsTail(boolean isTail) {
        this.isTail = isTail;
    }

    public int getArgsSize() {
        return argsSize;
    }

    public int getAllocaSize() {
        return allocaSize;
    }

    public int addAllocaSize(int size) {
        return allocaSize += size;
    }

    public void addUsedVirReg(AsmReg reg) {
        usedVirRegs.add(reg);
    }

    public CustomList getBlocks() {
        return blocks;
    }
}
