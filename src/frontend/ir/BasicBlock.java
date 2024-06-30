package frontend.ir;

import Utils.CustomList;
import frontend.ir.instr.Instruction;

import java.io.IOException;
import java.io.Writer;

public class BasicBlock extends CustomList.Node<BasicBlock> {
    private final CustomList<Instruction> instructions = new CustomList<>();
    
    public BasicBlock() {
    }
    
    public void addInstruction(Instruction instr) {
        instructions.addToTail(instr);
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }

        for (CustomList.Node<Instruction> instructionNode : instructions) {
            Instruction instruction = (Instruction) instructionNode;
            writer.append("\t").append(instruction.print()).append("\n");
        }
    }
}
