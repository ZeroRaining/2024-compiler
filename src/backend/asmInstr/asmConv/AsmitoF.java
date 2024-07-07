package backend.asmInstr.asmConv;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmOperand;

public class AsmitoF extends AsmInstr {
    private AsmOperand src;
    private AsmOperand dst;
    public AsmitoF(AsmOperand src, AsmOperand dst){
        this.src = src;
        this.dst = dst;
    }
}
