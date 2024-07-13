package backend.asmInstr;

import Utils.CustomList;
import backend.asmInstr.asmBinary.AsmBinary;
import backend.asmInstr.asmLS.AsmL;
import backend.asmInstr.asmLS.AsmS;
import backend.itemStructure.AsmOperand;
import backend.regs.AsmReg;

import java.util.ArrayList;
import java.util.HashSet;

public class AsmInstr extends CustomList.Node {
    /* 用于记录该指令的寄存器的定义和使用情况
    例如对于 add x1, x2, x3, 会有 regDef = {x1}, regUse = {x2, x3}
    引用新寄存器时需要释放旧寄存器的使用权
     */
    public HashSet<AsmReg> LiveIn = new HashSet<>();
    public HashSet<AsmReg> LiveOut = new HashSet<>();
    public ArrayList<AsmReg> regDef = new ArrayList<>();
    public ArrayList<AsmReg> regUse = new ArrayList<>();

    private String type;

    public AsmInstr(String type) {
        this.type = type;
    }

    public void addDefReg(AsmOperand oldReg, AsmOperand newReg) {
        if (oldReg != null && oldReg instanceof AsmReg)
            regDef.remove((AsmReg) oldReg);
        if (newReg instanceof AsmReg)
            regDef.add((AsmReg) newReg);
    }

    public void addUseReg(AsmOperand oldReg, AsmOperand newReg) {
        if (oldReg != null && oldReg instanceof AsmReg)
            regUse.remove((AsmReg) oldReg);
        if (newReg instanceof AsmReg)
            regUse.add((AsmReg) newReg);
    }

    public void changeUseReg(int index ,AsmReg oldReg, AsmReg newReg) {
        if (type == "AsmL") {
            if (this.regUse.get(index) == oldReg) {
                this.regUse.set(index, newReg);
                AsmL asmL = (AsmL) this;
                asmL.ReSetSrc(newReg);
            }
        } else if (type == "AsmS") {
            if (this.regUse.get(index) == oldReg) {
                this.regUse.set(index, newReg);
                AsmS asmS = (AsmS) this;
                asmS.ReSetSrc(index,newReg);
            }
        } else if (type == "AsmBinary") {
            if (this.regUse.get(index) == oldReg) {
                this.regUse.set(index, newReg);
                AsmBinary asmBinary = (AsmBinary) this;
                asmBinary.ReSetSrc(index, newReg);
            }
        }
    }
    public void changeDstReg(int index ,AsmReg oldReg, AsmReg newReg) {
        if (type == "AsmL") {
            if (this.regDef.get(index) == oldReg) {
                this.regDef.set(index, newReg);
                AsmL asmL = (AsmL) this;
                asmL.ReSetDst(newReg);
            }
        } else if (type == "AsmBinary") {
            if (this.regDef.get(index) == oldReg) {
                this.regDef.set(index, newReg);
                AsmBinary asmBinary = (AsmBinary) this;
                asmBinary.ReSetDst(newReg);
            }
        }
    }


}
