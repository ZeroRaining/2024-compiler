package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmSra extends AsmBinary{
    public boolean isWord = false;
    public AsmSra(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
