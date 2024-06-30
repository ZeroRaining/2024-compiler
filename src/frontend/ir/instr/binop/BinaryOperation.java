package frontend.ir.instr.binop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public abstract class BinaryOperation extends Instruction {
    private final int result; // 新分配一个寄存器用来存结果
    private final Value op1;
    private final Value op2;
    private final DataType type;
    private final String operationName;
    
    public BinaryOperation(int result, Value op1, Value op2, String operationName, DataType type, BasicBlock parentBB) {
        super(parentBB);
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
        this.type = type;
        this.operationName = operationName;
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
        return "%" + result + " = " +
                operationName + " " + type + " " +
                op1.value2string() + ", " + op2.value2string();
    }
}
