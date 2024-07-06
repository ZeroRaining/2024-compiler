package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmAnd extends AsmBinary{
    public AsmAnd(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
