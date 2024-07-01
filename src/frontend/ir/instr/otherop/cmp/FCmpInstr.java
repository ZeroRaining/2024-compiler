package frontend.ir.instr.otherop.cmp;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;

public class FCmpInstr extends Cmp {
    
    public FCmpInstr(int result, CmpCond cond, Value op1, Value op2, BasicBlock parentBB) {
        super(result, cond, op1, op2, parentBB);
        if (op1.getDataType() != DataType.FLOAT || op2.getDataType() != DataType.FLOAT) {
            throw new RuntimeException("浮点数比较必须是两个浮点数之间");
        }
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%").append(result).append(" = fcmp ");
        switch (cond) {
            case EQ: stringBuilder.append("oeq");
            case NE: stringBuilder.append("one");
            case GT: stringBuilder.append("ogt");
            case GE: stringBuilder.append("oge");
            case LT: stringBuilder.append("olt");
            case LE: stringBuilder.append("ole");
        }
        stringBuilder.append(" float ").append(op1.value2string()).append(", ").append(op2.value2string());
        return stringBuilder.toString();
    }
}
