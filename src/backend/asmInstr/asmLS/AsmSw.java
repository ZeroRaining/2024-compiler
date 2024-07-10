package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmSw extends AsmS{
    public AsmSw(AsmOperand src, AsmOperand addr, AsmOperand offset) {
        changeSrc(src);
        changeAddr(addr);
        changeOffset(offset);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sw\t");
        sb.append(src);
        sb.append(",\t");
        sb.append(offset);
        sb.append("(");
        sb.append(addr);
        sb.append(")");
        return sb.toString();
    }
}
