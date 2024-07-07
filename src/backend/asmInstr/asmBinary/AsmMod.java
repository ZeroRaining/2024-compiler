package backend.asmInstr.asmBinary;

import backend.itemStructure.AsmOperand;

public class AsmMod extends AsmBinary{
    public boolean isWord = false;
    public AsmMod(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
