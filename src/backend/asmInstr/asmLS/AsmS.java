package backend.asmInstr.asmLS;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;
public class AsmS extends AsmInstr {
    protected AsmOperand src, addr, offset;
    public void changeSrc(AsmOperand src) {
        addUseReg(this.src, src);
        this.src = src;
    }
    public void changeAddr(AsmOperand addr) {
        addUseReg(this.addr, addr);
        this.addr = addr;
    }
    public void changeOffset(AsmOperand offset) {
        addUseReg(this.offset, offset);
        this.offset = offset;
    }
}
