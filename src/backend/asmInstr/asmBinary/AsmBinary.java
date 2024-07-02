package backend.asmInstr.asmBinary;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmBinary extends AsmInstr {
    private AsmOperand dst, src1, src2;

    public AsmBinary(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        changeDef(dst);
        changeUse1(src1);
        changeUse2(src2);
    }

    public void changeDef(AsmOperand dst) {
        addDefReg(this.dst, dst);
        this.dst = dst;
    }

    public void changeUse1(AsmOperand src1) {
        addUseReg(this.src1, src1);
        this.src1 = src1;
    }

    public void changeUse2(AsmOperand src2) {
        addUseReg(this.src2, src2);
        this.src2 = src2;
    }
}
