package backend.asmInstr.asmTermin;

import backend.asmInstr.AsmInstr;

public class AsmCall extends AsmInstr {
    public String funcName;

    public AsmCall(String funcName) {
        this.funcName = funcName;
    }

    @Override
    public String toString() {
        return "call\t" + funcName;
    }
}
