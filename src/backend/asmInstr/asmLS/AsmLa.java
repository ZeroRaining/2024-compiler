package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmLa extends AsmL {
    public AsmLa(AsmOperand dst, AsmOperand src) {
        changeDst(dst);
        changeSrc(src);
    }

}
