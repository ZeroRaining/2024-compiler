package backend.asmInstr.asmLS;

import backend.itemStructure.AsmOperand;

public class AsmFsw extends AsmS{
    public AsmFsw(AsmOperand src, AsmOperand addr, AsmOperand offset) {
        changeSrc(src);
        changeAddr(addr);
        changeOffset(offset);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fsw\t");
        sb.append(src);
        sb.append(",\t");
        sb.append(offset);
        sb.append("(");
        sb.append(addr);
        sb.append(")");
        return sb.toString();
    }
}
