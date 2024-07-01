package backend.itemStructure;

import Utils.CustomList;
import backend.asmInstr.AsmInstr;

public class AsmBlock {
    private int index = 0;
    private CustomList.Node<AsmBlock> node = new CustomList.Node<>();
    private CustomList<AsmInstr> instrs=new CustomList<>();

    public AsmBlock(int index) {
        this.index = index;
    }
    public void addInstrHead(AsmInstr instr) {
        instrs.addToHead(new CustomList.Node<>(instr));
    }

    public void addInstrTail(AsmInstr instr) {
        instrs.addToTail(new CustomList.Node<>(instr));
    }

}
