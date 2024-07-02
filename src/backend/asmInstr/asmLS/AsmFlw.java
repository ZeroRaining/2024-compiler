package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmFlw extends AsmLS {
    public AsmFlw(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }
}
