package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmXor extends AsmBinary{
    public AsmXor(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
