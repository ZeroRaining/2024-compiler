package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmMove extends AsmL {
    public AsmMove(AsmOperand dst, AsmOperand src) {
        changeDst(dst);
        changeSrc(src);
    }
}
