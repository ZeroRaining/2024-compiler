package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;
import frontend.ir.symbols.Symbol;

public class StoreInstr extends MemoryOperation {
    private Value value;
    private Value ptr;
    
    public StoreInstr(Value value, Symbol symbol) {
        super(symbol);
        this.value = value;
        this.ptr = symbol.getAllocValue();
        setUse(value);
        setUse(ptr);
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
        return new StoreInstr(this.value, this.symbol, this.ptr);
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
        return "store " + typeName + " " + value.value2string() + ", " + typeName + "* " + ptr.value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (value == from) {
            value = to;
        } else if (ptr == from) {
            ptr = to;
        } else {
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
