package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.terminator.ReturnInstr;

import java.io.IOException;
import java.io.Writer;

public class BasicBlock extends Value {
    private final CustomList instructions = new CustomList();
    private int labelCnt;
    private boolean isRet;
    
    public BasicBlock() {
        super();
        isRet = false;
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
    public Number getValue() {
        throw new RuntimeException("基本块暂时没有值");
    }
    
    @Override
    public DataType getDataType() {
        throw new RuntimeException("基本块暂时没有数据类型");
    }
    
    @Override
    public String value2string() {
        throw new RuntimeException("基本块暂时没有值");
    }
}
