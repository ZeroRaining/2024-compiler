package backend.itemStructure;

import Utils.CustomList;

public class AsmFunction {
    private CustomList<AsmBlock, AsmFunction> blocks;
    int size;
    boolean isTail;

    public AsmFunction() {
        blocks = new CustomList<>(this);
        size = 0;
        isTail = false;
    }

    public void addBlock(AsmBlock block) {
        blocks.addToTail(new CustomList.Node<>(block, this));
    }

    public void addStackSize(int size) {
        this.size += size;
        this.size = (this.size % 8 == 0) ? this.size : this.size + 4;
    }

    public void setIsTail(boolean isTail) {
        this.isTail = isTail;
    }
}
