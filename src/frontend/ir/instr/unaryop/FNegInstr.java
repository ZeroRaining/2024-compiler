package frontend.ir.instr.unaryop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class FNegInstr extends Instruction {
    private final int result;
    private Value value;
    
    public FNegInstr(int result, Value value, BasicBlock parentBB) {
        super(parentBB);
        this.result = result;
        this.value = value;
        setUse(value);
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }
    
    @Override
    public String print() {
        return "%reg_" + result + " = fneg float " + value.value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (value == from) {
            value = to;
        } else {
            throw new RuntimeException();
        }
    }
}
