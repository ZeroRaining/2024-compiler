package backend.asmInstr;

import Utils.CustomList;
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

}
