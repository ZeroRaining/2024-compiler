package backend.asmInstr.asmLS;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmL extends AsmLS {
    protected AsmOperand dst, src, offset;
    public void changeDst(AsmOperand dst) {
        addDefReg(this.dst, dst);
        this.dst = dst;
    }
    public void changeSrc(AsmOperand src) {
        addUseReg(this.src, src);
        this.src = src;
    }

    public void changeOffset(AsmOperand offset) {
        addUseReg(this.offset, offset);
        this.offset = offset;
    }
}
