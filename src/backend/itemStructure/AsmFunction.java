package backend.itemStructure;

import Utils.CustomList;

public class AsmFunction {
    private CustomList<AsmBlock, AsmFunction> blocks;
    public void addBlock(AsmBlock block) {
        blocks.add(block);
    }
}
