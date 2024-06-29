package frontend.ir.constvalue;

public class ConstFloat implements ConstValue {
    private float value;
    public ConstFloat(float init) {
        value = init;
    }
    
    @Override
    public Float getValue() {
        return value;
    }
}
