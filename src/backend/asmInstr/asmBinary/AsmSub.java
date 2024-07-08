package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmSub extends AsmBinary{
    public boolean isWord = false;
    public AsmSub(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sub");
        if(isWord) sb.append("w");
        sb.append("\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src1);
        sb.append(",\t");
        sb.append(src2);
        return sb.toString();
    }
}
