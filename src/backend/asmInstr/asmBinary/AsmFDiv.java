package backend.asmInstr.asmBinary;
import backend.itemStructure.AsmOperand;

public class AsmFDiv extends AsmBinary{
    public AsmFDiv(AsmOperand dst, AsmOperand src1, AsmOperand src2) {
        super(dst, src1, src2);
    }
}
