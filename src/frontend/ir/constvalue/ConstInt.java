package frontend.ir.constvalue;

import frontend.ir.DataType;

import java.util.Objects;

public class ConstInt extends ConstValue {
    private final int value;
    public ConstInt(int init) {
        value = init;
    }
    
    @Override
    public Integer getNumber() {
        return value;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.INT;
    }
    
    @Override
    public String value2string() {
        return Integer.toString(this.value);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConstInt)) {
            return false;
        }
        
        return this.value == ((ConstInt) other).value;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
