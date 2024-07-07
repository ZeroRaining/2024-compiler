package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmFlw extends AsmL {
    public AsmFlw(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }
}
