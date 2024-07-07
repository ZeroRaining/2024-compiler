package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.Value;
import frontend.ir.symbols.Symbol;

public class StoreInstr extends MemoryOperation {
    private Value value;
    private Value ptr = null;
    
    public StoreInstr(Value value, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.value = value;
        setUse(value);
        setUse(symbol.getAllocValue());
    }
    
    public StoreInstr(Value value, Symbol symbol, Value ptr, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.value = value;
        this.ptr = ptr;
        setUse(value);
        setUse(ptr);
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
        String base = "store " + symbol.getType() + " " + value.value2string() +
                ", " + symbol.getType() + "* ";
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

    public Symbol getSymbol() {
        return symbol;
    }
}
