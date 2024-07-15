package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.symbols.Symbol;

import java.util.Objects;


public class AllocaInstr extends MemoryOperation {
    private final int result;
    
    public AllocaInstr(int result, Symbol symbol) {
        super(symbol);
        this.result = result;
        symbol.setAllocValue(this);
        this.pointerLevel = symbol.getDim() + 1;
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public String print() {
        if (symbol.isArray()) {
            return "%reg_" + result + " = alloca " + symbol.printArrayTypeName();
        } else {
            return "%reg_" + result + " = alloca " + getDataType();
        }
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AllocaInstr)) {
            return false;
        }
        
        return this.result == ((AllocaInstr) other).result;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(result);
    }
}
