package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmImm12;
import backend.itemStructure.AsmOperand;

public class AsmXor extends AsmBinary{
    public AsmXor(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("xor");
        if(src2 instanceof AsmImm12) sb.append("i");
        sb.append("\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src1);
        sb.append(",\t");
        sb.append(src2);
        return sb.toString();
    }
}
