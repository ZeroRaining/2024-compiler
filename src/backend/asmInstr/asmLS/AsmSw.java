package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmSw extends AsmS{
    public AsmSw(AsmOperand src, AsmOperand addr, AsmOperand offset) {
        changeSrc(src);
        changeAddr(addr);
        changeOffset(offset);
    }
}
