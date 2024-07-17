package frontend.ir.instr.terminator;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class ReturnInstr extends Instruction {
    private final DataType returnType;
    private Value returnValue;
    
    public ReturnInstr(DataType returnType) {
        if (returnType != DataType.VOID) {
            throw new RuntimeException("在应该返回值的函数中没有返回值");
        }
        this.returnType = returnType;
        returnValue = null;
    }
    
    /**
     * 保证了传入的 Value 的数据类型与要求的返回值类型一致
     */
    public ReturnInstr(DataType returnType, Value returnValue) {
        if (returnType == DataType.VOID) {
            throw new RuntimeException("在不该返回值的函数中返回值了");
        }
        this.returnType = returnType;
        this.returnValue = returnValue;
        setUse(returnValue);
    }
    
    @Override
    public Integer getNumber() {
        return -1;
    }
    
    @Override
    public DataType getDataType() {
        return returnType;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder("ret ");
        stringBuilder.append(returnType).append(" ");
        if (returnType == DataType.VOID) {
            return stringBuilder.toString();
        }
        assert returnValue != null;
        stringBuilder.append(returnValue.value2string());
        
        return stringBuilder.toString();
    }

    public Value getReturnValue() {
        return returnValue;
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (returnValue == from) {
            returnValue = to;
        } else {
            throw new RuntimeException();
        }
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
    
    @Override
    public String myHash() {
        return Integer.toString(this.hashCode());
    }
}
