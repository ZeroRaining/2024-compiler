package frontend.ir.instr.convop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

import javax.naming.Name;

public abstract class ConversionOperation extends Instruction {
    private final int result;
    private final DataType form;
    private final DataType to;
    private final Value value;
    private final String opName;
    
    public ConversionOperation(int result, DataType form, DataType to, Value value, String name) {
        this.result = result;
        this.form   = form;
        this.to     = to;
        this.value  = value;
        this.opName = name;
    }
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return to;
    }
    
    @Override
    public String print() {
        return "%" + result + " = " +
                opName + " " + form + " " +
                value.value2string() + " to " + to;
    }
}
