package backend.asmInstr.asmBr;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmOperand;

public class AsmBnez extends AsmInstr {
    private AsmOperand cond;
    private AsmBlock target;
    public AsmBnez(AsmOperand cond, AsmBlock target) {
        this.cond = cond;
        this.target = target;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("bnez\t");
        sb.append(cond);
        sb.append(",\t");
        sb.append(target);
        return sb.toString();
    }
}
