package frontend.ir.constvalue;

public class ConstInt implements ConstValue {
    private int value;
    public ConstInt(int init) {
        value = init;
    }
    
    @Override
    public Integer getValue() {
        return value;
    }
}
