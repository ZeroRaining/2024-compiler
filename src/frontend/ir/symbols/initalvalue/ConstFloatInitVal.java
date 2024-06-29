package frontend.ir.symbols.initalvalue;

public class ConstFloatInitVal extends InitVal {
    private float value;
    public ConstFloatInitVal(float init) {
        value = init;
    }
    
    @Override
    public Float getValue() {
        return value;
    }
}
