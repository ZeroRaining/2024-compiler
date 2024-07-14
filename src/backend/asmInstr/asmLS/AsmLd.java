package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmLd extends AsmL {
    public AsmLd(AsmOperand dst, AsmOperand src, AsmOperand offset) {
        changeDst(dst);
        changeSrc(src);
        changeOffset(offset);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append("ld\t");
        sb.append("lw\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(offset);
        sb.append("(");
        sb.append(src);
        sb.append(")");
        return sb.toString();
    }
}
