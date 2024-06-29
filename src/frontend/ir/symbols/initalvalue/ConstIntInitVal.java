package frontend.ir.symbols.initalvalue;

public class ConstIntInitVal extends InitVal {
    private int value;
    public ConstIntInitVal(int init) {
        value = init;
    }
    
    @Override
    public Integer getValue() {
        return value;
    }
}
