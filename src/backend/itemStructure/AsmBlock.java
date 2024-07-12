package backend.itemStructure;

import Utils.CustomList;
import Utils.CustomList.Node;
import backend.asmInstr.AsmInstr;
import backend.regs.AsmReg;
import frontend.ir.Value;
import frontend.ir.instr.binop.AddInstr;
import frontend.ir.structure.BasicBlock;

import java.util.HashSet;

public class AsmBlock extends Node {
    private String name;
    private int index;

    private CustomList instrs = new CustomList();
    public AsmBlock(String name) {
        this.name = name;
    }
    public HashSet<AsmBlock> sucs = new HashSet<>();
    public HashSet<AsmBlock> pres = new HashSet<>();
    public HashSet<AsmBlock> doms = new HashSet<>();
    public HashSet<AsmBlock> iDoms = new HashSet<>();
    private HashSet<AsmReg> use = new HashSet<>();
    private HashSet<AsmReg> def = new HashSet<>();

    public HashSet<AsmReg> LiveIn = new HashSet<>();
    public HashSet<AsmReg> LiveOut = new HashSet<>();
    public HashSet<AsmReg> getDef() {
        return def;
    }

    public HashSet<AsmReg> getUse() {
        return use;
    }

    public AsmBlock(int index) {
        this.index = index;
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
}
