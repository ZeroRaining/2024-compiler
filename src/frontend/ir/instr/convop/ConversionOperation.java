package frontend.ir.instr.convop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public abstract class ConversionOperation extends Instruction {
    private final int result;
    private final DataType from;
    private final DataType to;
    private Value value;
    private final String opName;
    
    public ConversionOperation(int result, DataType from, DataType to, Value value, String name, BasicBlock parentBB) {
        super(parentBB);
        this.result = result;
        this.from = from;
        this.to     = to;
        this.value  = value;
        this.opName = name;
        setUse(value);
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return to;
    }
    
    @Override
    public String print() {
        return "%reg_" + result + " = " +
                opName + " " + from + " " +
                value.value2string() + " to " + to;
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
