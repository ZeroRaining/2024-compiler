package frontend.ir.instr.binop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

import java.util.Objects;

public abstract class BinaryOperation extends Instruction {
    private final int result; // 新分配一个寄存器用来存结果
    protected Value op1;
    protected Value op2;
    private final DataType type;
    private final String operationName;
    
    public BinaryOperation(int result, Value op1, Value op2, String operationName, DataType type) {
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
        this.type = type;
        this.operationName = operationName;
        setUse(op1);
        setUse(op2);
    }
    
    @Override
    public Integer getNumber() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return type;
    }
    
    @Override
    public String print() {
        return "%reg_" + result + " = " +
                operationName + " " + type + " " +
                op1.value2string() + ", " + op2.value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        boolean ok = false;
        if (op1 == from) {
            op1 = to;
            ok = true;
        }
        if (op2 == from) {
            op2 = to;
            ok = true;
        }
        if (ok) {
            return;
        }
        throw new RuntimeException();
    }

    public Value getOp1() {
        return op1;
    }
    public Value getOp2() {
        return op2;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BinaryOperation)) {
            return false;
        }
        
        if (this.result == ((BinaryOperation) other).result) {
            return true;
        }
        
        boolean check1 = this.op1.equals(((BinaryOperation) other).op1);
        boolean check2 = this.op2.equals(((BinaryOperation) other).op2);
        boolean check3 = this.operationName.equals(((BinaryOperation) other).operationName);
        
        return check1 && check2 && check3;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(op1, op2, operationName);
    }
}
