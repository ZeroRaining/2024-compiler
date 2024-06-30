package frontend.ir.constvalue;

import frontend.ir.DataType;

public class ConstFloat implements ConstValue {
    private final float value;
    public ConstFloat(float init) {
        value = init;
    }
    
    @Override
    public Float getValue() {
        return value;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }
}
