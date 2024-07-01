package backend.itemStructure;

import Utils.CustomList;
import backend.regs.AsmReg;

import java.util.HashSet;

public class AsmFunction {
    private CustomList<AsmBlock> blocks=new CustomList<>();
    private HashSet<AsmReg> usedVirRegs=new HashSet<>();
    int size=0;
    boolean isTail=false;

    public void addBlock(AsmBlock block) {
        blocks.addToTail(new CustomList.Node<>(block));
    }

    public void addStackSize(int size) {
        this.size += size;
        this.size = (this.size % 8 == 0) ? this.size : this.size + 4;
    }

    public void setIsTail(boolean isTail) {
        this.isTail = isTail;
    }
    public void addUsedVirReg(AsmReg reg) {
        usedVirRegs.add(reg);
    }
}
