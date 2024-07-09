package backend.asmInstr.asmBr;

import backend.asmInstr.AsmInstr;
import backend.itemStructure.AsmBlock;

public class AsmJ extends AsmInstr {
    private AsmBlock target;
    public AsmJ(AsmBlock target) {
        this.target = target;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("j\t");
        sb.append(target);
        return sb.toString();
    }
}
