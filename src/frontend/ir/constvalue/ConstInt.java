package frontend.ir.constvalue;

import frontend.ir.DataType;

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
}
