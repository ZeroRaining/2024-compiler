package backend.asmBr;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmBlock;

public class AsmJ extends AsmInstr {
    private AsmBlock target;
    public AsmJ(AsmBlock target) {
        this.target = target;
    }
}
