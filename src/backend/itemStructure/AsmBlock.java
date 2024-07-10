package backend.itemStructure;

import Utils.CustomList;
import Utils.CustomList.Node;
import backend.asmInstr.AsmInstr;
import frontend.ir.instr.binop.AddInstr;

public class AsmBlock extends Node {
    private String name;
    private CustomList instrs = new CustomList();

    public AsmBlock(String name) {
        this.name = name;
    }

    public void addInstrHead(AsmInstr instr) {
        instrs.addToHead(instr);
    }

    public void addInstrTail(AsmInstr instr) {
        instrs.addToTail(instr);
    }

    public CustomList getInstrs() {
        return instrs;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

}
