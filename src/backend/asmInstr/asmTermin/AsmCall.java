package backend.asmInstr.asmTermin;

import backend.asmInstr.AsmInstr;
import backend.regs.RegGeter;

public class AsmCall extends AsmInstr {
    public String funcName;

    public AsmCall(String funcName, int IntArgRegNum,int floatArgRegNum) {
        super("AsmCall");
        this.funcName = funcName;
        for (int i = 0 ; i < IntArgRegNum ; i++) {
            addUseReg(null, RegGeter.AllRegsInt.get(i + 10));
        }
        for (int i = 0 ; i < floatArgRegNum ; i++) {
            addUseReg(null, RegGeter.AllRegsFloat.get(i + 10));
        }
    }

    public String getFuncName() {
        return funcName;
    }

    @Override
    public String toString() {
        return "call\t" + funcName;
    }
}
