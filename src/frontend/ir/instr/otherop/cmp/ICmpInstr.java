package frontend.ir.instr.otherop.cmp;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;

public class ICmpInstr extends Cmp {
    public ICmpInstr(int result, CmpCond cond, Value op1, Value op2, BasicBlock parentBB) {
        super(result, cond, op1, op2, parentBB);
        if (op1.getDataType() != DataType.INT || op2.getDataType() != DataType.INT) {
            throw new RuntimeException("整数比较必须是两个三十二位整数之间");
        }
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%").append(result).append(" = icmp ");
        switch (cond) {
            case EQ: stringBuilder.append("eq");
            case NE: stringBuilder.append("ne");
            case GT: stringBuilder.append("sgt");
            case GE: stringBuilder.append("sge");
            case LT: stringBuilder.append("slt");
            case LE: stringBuilder.append("sle");
        }
        stringBuilder.append(" i32 ").append(op1.value2string()).append(", ").append(op2.value2string());
        return stringBuilder.toString();
    }
}
