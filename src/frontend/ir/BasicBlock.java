package frontend.ir;

import frontend.ir.instr.Instruction;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class BasicBlock {
    private final int label;
    private final ArrayList<Instruction> instructions = new ArrayList<>();
    
    public BasicBlock(int label) {
        this.label = label;
    }
    
    public int getLabel() {
        return label;
    }
    
    public void addInstruction(Instruction instr) {
        instructions.add(instr);
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        
        for (Instruction instruction : instructions) {
            writer.append("\t").append(instruction.print()).append("\n");
        }
    }
}
