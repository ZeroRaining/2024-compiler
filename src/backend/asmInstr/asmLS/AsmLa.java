package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmLa extends AsmL {
    public AsmLa(AsmOperand dst, AsmOperand src) {

        changeDst(dst);
        changeSrc(src);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("la\t");
        sb.append(dst);
        sb.append(",\t");
        sb.append(src);
        return sb.toString();
    }
}
