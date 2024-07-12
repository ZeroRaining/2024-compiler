package backend.asmInstr.asmTermin;

import backend.asmInstr.AsmInstr;

public class AsmCall extends AsmInstr {
    public String funcName;

    public AsmCall(String funcName) {
        super("AsmCall");
        this.funcName = funcName;
    }

    @Override
    public String toString() {
        return "call\t" + funcName;
    }
}
