package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmMul extends AsmBinary{
    public boolean isWord = false;
    public AsmMul(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
