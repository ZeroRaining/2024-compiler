package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmSd extends AsmL {
    public AsmSd(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }
}
