package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.Value;
import frontend.ir.symbols.Symbol;

public class StoreInstr extends MemoryOperation {
    private Value value;
    
    public StoreInstr(Value value, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.value = value;
        setUse(value);
        setUse(symbol.getAllocValue());
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
        } else {
            throw new RuntimeException();
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }
}
