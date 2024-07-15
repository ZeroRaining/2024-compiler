package frontend.ir.structure;

import Utils.CustomList;
import debug.DEBUG;
import frontend.ir.DataType;
import frontend.ir.Use;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.AllocaInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;

import javax.tools.JavaCompiler;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
    
    public List<AllocaInstr> popAllAlloca() {
        Instruction instr = (Instruction) instructions.getHead();
        ArrayList<AllocaInstr> allocaList = new ArrayList<>();
        while (instr != null) {
            if (instr instanceof AllocaInstr) {
                allocaList.add((AllocaInstr) instr);
                instr.removeFromList();
            }
            instr = (Instruction) instr.getNext();
        }
        return allocaList;
    }
    
    public void reAddAllAlloca(List<AllocaInstr> allocaInstrList) {
        if (allocaInstrList == null) {
            throw new NullPointerException();
        }
        for (int i = allocaInstrList.size() - 1; i >= 0; i--) {
            instructions.addToHead(allocaInstrList.get(i));
        }
    }

    public void addInstruction(Instruction instr) {
        //removeFromList销毁所有setUse
        instructions.addToTail(instr);
        if (isRet) {
            instr.removeFromList();
            return;
        }
        if (instr instanceof ReturnInstr || instr instanceof JumpInstr || instr instanceof BranchInstr) {
            isRet = true;
        }
        instr.setParentBB(this);
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
           Instruction instruction =  use.getUser();
            BasicBlock block = instruction.getParentBB();
            DEBUG.dbgPrint1(block.value2string());
        }
        printHashset(sucs, "sucs");
        printHashset(pres, "pres");
        printHashset(doms, "doms");
        printHashset(iDoms, "iDoms");
        printHashset(DF, "DF");
        DEBUG.dbgPrint1("");
    }

    @Override
    public void removeFromList() {
        super.removeFromList();
        Instruction instr = (Instruction) instructions.getHead();
        while (instr != null) {
            instr.removeFromList();
            instr = (Instruction) instr.getNext();
        }
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BasicBlock)) {
            return false;
        }
        
        return this.labelCnt == ((BasicBlock) other).labelCnt;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(labelCnt);
    }
}
