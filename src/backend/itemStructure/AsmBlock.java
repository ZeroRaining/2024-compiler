package backend.itemStructure;

import Utils.CustomList;
import Utils.CustomList.Node;
import backend.asmInstr.AsmInstr;
import frontend.ir.instr.binop.AddInstr;

public class AsmBlock extends Node{
    private int index = 0;
    private CustomList instrs=new CustomList();

    public AsmBlock(int index) {
        this.index = index;
    }
    public void addInstrHead(AsmInstr instr) {
        instrs.addToHead(instr);
    }

    public void addInstrTail(AsmInstr instr) {
        instrs.addToTail(instr);
    }
    public int getIndex() {
        return index;
    }

    public CustomList getInstrs() {
        return instrs;
    }

}
