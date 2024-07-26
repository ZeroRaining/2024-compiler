package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;
import frontend.ir.symbols.Symbol;

public class StoreInstr extends MemoryOperation {
    private Value value;
    private Value ptr = null;
    
    public StoreInstr(Value value, Symbol symbol) {
        super(symbol);
        this.value = value;
        setUse(value);
        setUse(symbol.getAllocValue());
    }
    
    public StoreInstr(Value value, Symbol symbol, Value ptr) {
        super(symbol);
        this.value = value;
        this.ptr = ptr;
        setUse(value);
        setUse(ptr);
    }
    
    @Override
    public Instruction cloneShell(Function parentFunc) {
        if (ptr == null) {
            return new StoreInstr(this.value, this.symbol);
        } else {
            return new StoreInstr(this.value, this.symbol, this.ptr);
        }
    }

    //store.getValue = value in storeInstr
    @Override
    public Number getNumber() {
        return value.getNumber();
    }

    public Value getValue() {
        return value;
    }
    
    @Override
    public String print() {
        String typeName;
        if (ptr != null && ptr.getPointerLevel() == 1) {
            typeName = symbol.getType().toString();
        } else {
            typeName = printBaseType();
        }
        String base = "store " + typeName + " " + value.value2string() +
                ", " + typeName + "* ";
        if (ptr != null) {
            return base + ptr.value2string();
        }
        if (symbol.isGlobal()) {
            return base + "@" + symbol.getName();
        } else {
            return base + symbol.getAllocValue().value2string();
        }
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (value == from) {
            value = to;
        } else if (ptr == from) {
            ptr = to;
        }else {
            throw new RuntimeException();
        }
    }
    
    public Value getPtr() {
        return ptr;
    }
    
    @Override
    public String myHash() {
        return Integer.toString(this.hashCode());
    }
}
