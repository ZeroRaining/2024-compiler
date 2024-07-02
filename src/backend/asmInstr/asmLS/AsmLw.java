package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;
public class AsmLw extends AsmLS {
    public AsmLw(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }
}
