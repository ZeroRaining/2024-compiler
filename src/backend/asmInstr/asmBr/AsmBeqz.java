package backend.asmInstr.asmBr;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmOperand;

public class AsmBeqz extends AsmInstr {
    private AsmOperand cond;
    private AsmBlock target;
    public AsmBeqz(AsmOperand cond, AsmBlock target) {
        super("AsmBeqz");
        this.cond = cond;
        this.target = target;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("beqz\t");
        sb.append(cond);
        sb.append(",\t");
        sb.append(target);
        return sb.toString();
    }
}
