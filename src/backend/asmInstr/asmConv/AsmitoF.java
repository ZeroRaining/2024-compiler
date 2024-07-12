package backend.asmInstr.asmConv;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmitoF extends AsmInstr {
    private AsmOperand src;
    private AsmOperand dst;
    public AsmitoF(AsmOperand src, AsmOperand dst){
        super("AsmitoF");
        this.src = src;
        this.dst = dst;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fcvt.s.w");
        sb.append("\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src);
        return sb.toString();
    }
}
