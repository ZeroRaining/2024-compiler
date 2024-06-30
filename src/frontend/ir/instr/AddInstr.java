package frontend.ir.instr;

import frontend.ir.DataType;
import frontend.ir.Value;

/**
 * result = op1 + op2
 */
public class AddInstr extends Instruction {
    private final int result; // 新分配一个寄存器用来存结果
    private final Value op1;
    private final Value op2;
    private final DataType type;
    
    public AddInstr(int result, Value op1, Value op2) {
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
        if (op1 == null || op2 == null) {
            throw new NullPointerException();
        }
        DataType type1 = op1.getDataType();
        DataType type2 = op2.getDataType();
        assert type1 != DataType.VOID && type2 != DataType.VOID;
        if (type1 == DataType.FLOAT || type2 == DataType.FLOAT) {
            type = DataType.FLOAT;
        } else {
            type = DataType.INT;
        }
    }
    
    @Override
    public Integer getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return type;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%").append(result).append(" = add ").append(type).append(" ");
        if (op1 instanceof Instruction) {
            stringBuilder.append("%");
        }
        stringBuilder.append(op1.getValue());
        stringBuilder.append(", ");
        if (op2 instanceof Instruction) {
            stringBuilder.append("%");
        }
        stringBuilder.append(op2.getValue());
        return stringBuilder.toString();
    }
}
