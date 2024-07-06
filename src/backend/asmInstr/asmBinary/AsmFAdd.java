package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmFAdd extends AsmBinary{
    public AsmFAdd(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
