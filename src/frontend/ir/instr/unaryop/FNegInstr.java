package frontend.ir.instr.unaryop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class FNegInstr extends Instruction {
    private final int result;
    private final Value value;
    
    public FNegInstr(int result, Value value) {
        this.result = result;
        this.value = value;
    }
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }
    
    @Override
    public String print() {
        return "%" + result + " = fneg float " + value.value2string();
    }
}
