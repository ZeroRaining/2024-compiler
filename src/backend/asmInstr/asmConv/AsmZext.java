package backend.asmInstr.asmConv;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmZext extends AsmInstr {
    private AsmOperand src;
    private AsmOperand dst;
    public AsmZext(AsmOperand src, AsmOperand dst){
        this.src = src;
        this.dst = dst;
    }
}
