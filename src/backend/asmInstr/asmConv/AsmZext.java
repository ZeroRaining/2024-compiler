package backend.asmInstr.asmConv;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmZext extends AsmInstr {
    private AsmOperand src;
    private AsmOperand dst;
    public AsmZext(AsmOperand src, AsmOperand dst){
        this.src = src;
        this.dst = dst;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("zext");
        sb.append("\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src);
        return sb.toString();
    }
}
