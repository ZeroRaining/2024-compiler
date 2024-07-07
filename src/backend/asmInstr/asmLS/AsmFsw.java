package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmFsw extends AsmS{
    public AsmFsw(AsmOperand src, AsmOperand addr, AsmOperand offset) {
        changeSrc(src);
        changeAddr(addr);
        changeOffset(offset);
    }
}
