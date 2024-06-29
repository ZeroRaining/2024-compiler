package backend.itemStructure;

import Utils.CustomList;

public class AsmBlock {
    private int index;
    private CustomList.Node<AsmBlock, AsmFunction> node;

    public AsmBlock(int index) {
        this.index = index;
    }
}
