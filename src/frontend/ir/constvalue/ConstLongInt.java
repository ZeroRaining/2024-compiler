package frontend.ir.constvalue;

import frontend.ir.DataType;

import java.util.Objects;

public class ConstLongInt extends ConstValue {
    private final int value;
    
    public ConstLongInt(int init) {
        value = init;
    }
    
    @Override
    public Integer getNumber() {
        return value;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.LONG_INT;
    }
    
    @Override
    public String value2string() {
        return Integer.toString(this.value);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConstLongInt)) {
            return false;
        }
        
        return this.value == ((ConstLongInt) other).value;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
