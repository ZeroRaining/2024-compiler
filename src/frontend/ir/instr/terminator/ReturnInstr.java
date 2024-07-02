package frontend.ir.instr.terminator;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class ReturnInstr extends Instruction {
    private final DataType returnType;
    private final Value returnValue;
    
    public ReturnInstr(DataType returnType, BasicBlock parentBB) {
        super(parentBB);
        if (returnType != DataType.VOID) {
            throw new RuntimeException("在应该返回值的函数中没有返回值");
        }
        this.returnType = returnType;
        returnValue = null;
    }
    
    /**
     * 保证了传入的 Value 的数据类型与要求的返回值类型一致
     */
    public ReturnInstr(DataType returnType, Value returnValue, BasicBlock parentBB) {
        super(parentBB);
        if (returnType == DataType.VOID) {
            throw new RuntimeException("在不该返回值的函数中返回值了");
        }
        this.returnType = returnType;
        this.returnValue = returnValue;
    }
    
    @Override
    public Integer getValue() {
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
}