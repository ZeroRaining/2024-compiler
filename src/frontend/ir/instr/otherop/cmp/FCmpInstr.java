package frontend.ir.instr.otherop.cmp;

import frontend.ir.DataType;
import frontend.ir.Value;

public class FCmpInstr extends Cmp {
    
    public FCmpInstr(int result, CmpCond cond, Value op1, Value op2) {
        super(result, cond, op1, op2);
        if (op1.getDataType() != DataType.FLOAT || op2.getDataType() != DataType.FLOAT) {
            throw new RuntimeException("浮点数比较必须是两个浮点数之间");
        }
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%reg_").append(result).append(" = fcmp ");
        switch (cond) {
            case EQ: stringBuilder.append("oeq"); break;
            case NE: stringBuilder.append("one"); break;
            case GT: stringBuilder.append("ogt"); break;
            case GE: stringBuilder.append("oge"); break;
            case LT: stringBuilder.append("olt"); break;
            case LE: stringBuilder.append("ole"); break;
            default: throw new RuntimeException("还有高手？");
        }
        stringBuilder.append(" float ").append(op1.value2string()).append(", ").append(op2.value2string());
        return stringBuilder.toString();
    }
}
