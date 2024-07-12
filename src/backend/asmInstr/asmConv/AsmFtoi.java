package backend.asmInstr.asmConv;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmFtoi extends AsmInstr {
    private AsmOperand src;
    private AsmOperand dst;
    public AsmFtoi(AsmOperand src, AsmOperand dst){
        super("AsmFtoi");
        this.src = src;
        this.dst = dst;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fcvt.w.s");
        sb.append("\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src);
        sb.append(",\trtz");
        return sb.toString();
    }
}
