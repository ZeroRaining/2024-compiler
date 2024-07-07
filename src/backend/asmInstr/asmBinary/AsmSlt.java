package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmSlt extends AsmBinary {
    public AsmSlt(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
