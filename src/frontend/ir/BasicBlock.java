package frontend.ir;

import Utils.CustomList;
import frontend.ir.instr.Instruction;

import java.io.IOException;
import java.io.Writer;

public class BasicBlock extends Value {
    private final CustomList instructions = new CustomList();
    
    public BasicBlock() {
    }
    
    public void addInstruction(Instruction instr) {
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
