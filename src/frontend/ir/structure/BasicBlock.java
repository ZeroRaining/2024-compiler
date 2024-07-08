package frontend.ir.structure;

import Utils.CustomList;
import debug.DEBUG;
import frontend.ir.DataType;
import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.ReturnInstr;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;

public class BasicBlock extends Value {
    private final CustomList instructions = new CustomList();
    private int labelCnt;
    private int depth;
    private boolean isRet;
    private HashSet<BasicBlock> pres;
    private HashSet<BasicBlock> sucs;
    private HashSet<BasicBlock> doms;
    private HashSet<BasicBlock> iDoms;
    private HashSet<BasicBlock> DF;
    
    public BasicBlock(int depth) {
        super();
        isRet = false;
        this.depth = depth;
        pres = new HashSet<>();
        sucs = new HashSet<>();
        doms = new HashSet<>();
        iDoms = new HashSet<>();
        DF = new HashSet<>();
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Instruction getEndInstr() {
        return (Instruction) instructions.getTail();
    }

    public void setDF(HashSet<BasicBlock> DF) {
        this.DF = DF;
    }

    public void setIDoms(HashSet<BasicBlock> iDoms) {
        this.iDoms = iDoms;
    }

    public void setPres(HashSet<BasicBlock> pres) {
        this.pres = pres;
    }

    public void setSucs(HashSet<BasicBlock> sucs) {
        this.sucs = sucs;
    }

    public void setDoms(HashSet<BasicBlock> doms) {
        this.doms = doms;
    }
    public HashSet<BasicBlock> getIDoms() {
        return iDoms;
    }

    public HashSet<BasicBlock> getDF() {
        return DF;
    }

    public HashSet<BasicBlock> getPres() {
        return pres;
    }

    public HashSet<BasicBlock> getSucs() {
        return sucs;
    }

    public HashSet<BasicBlock> getDoms() {
        return doms;
    }

    public void setLabelCnt(int labelCnt) {
        this.labelCnt = labelCnt;
    }

    public int getLabelCnt() {
        return labelCnt;
    }

    public void addInstruction(Instruction instr) {
        if (isRet) {
            return;
        }
        if (instr instanceof ReturnInstr) {
            isRet = true;
        }
        instr.setParentBB(this);
        instructions.addToTail(instr);
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }

        for (CustomList.Node instructionNode : instructions) {
            Instruction instruction = (Instruction) instructionNode;
            writer.append("\t").append(instruction.print()).append("\n");
        }
    }
    
    @Override
    public Number getNumber() {
        throw new RuntimeException("基本块暂时没有值");
    }
    
    @Override
    public DataType getDataType() {
        throw new RuntimeException("基本块暂时没有数据类型");
    }
    
    @Override
    public String value2string() {
        return "blk_" + labelCnt;
    }

    public CustomList getInstructions() {
        return instructions;
    }

    public void printHashset(HashSet<BasicBlock> hashSet, String s) {
        StringBuilder str = new StringBuilder(s);
        str.append(": ");
        for(BasicBlock b : hashSet) {
            str.append(b.value2string()).append(" ");
        }
        DEBUG.dbgPrint2(String.valueOf(str));
    }

    public void printDBG() {
        StringBuilder str = new StringBuilder();
        DEBUG.dbgPrint("used: ");
        Use use = this.getBeginUse();
        if (use == null) {
            DEBUG.dbgPrint1("chao???");
        } else {
            DEBUG.dbgPrint1((use.getUser()).getParentBB().value2string());
        }
        printHashset(sucs, "sucs");
        printHashset(pres, "pres");
        printHashset(doms, "doms");
        printHashset(iDoms, "iDoms");
        printHashset(DF, "DF");
        DEBUG.dbgPrint1("");
    }
}
