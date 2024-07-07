package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmLd extends AsmL {
    public AsmLd(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }
}
