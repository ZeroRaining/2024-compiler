package backend.asmBr;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmOperand;

public class AsmBeqz extends AsmInstr {
    private AsmOperand cond;
    private AsmBlock target;
    public AsmBeqz(AsmOperand cond, AsmBlock target) {
        this.cond = cond;
        this.target = target;
    }
}
