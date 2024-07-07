package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmSd extends AsmL {
    public AsmSd(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sd\t");
        sb.append(src);
        sb.append(",\t");
        sb.append(offset);
        sb.append("(");
        sb.append(dst);
        sb.append(")");
        return sb.toString();
    }
}
