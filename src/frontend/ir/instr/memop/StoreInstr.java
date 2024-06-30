package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.Value;
import frontend.ir.symbols.Symbol;

public class StoreInstr extends MemoryOperation {
    private final Value value;
    
    public StoreInstr(Value value, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.value = value;
    }
    
    @Override
    public Number getValue() {
        return -1;
    }
    
    @Override
    public String print() {
        String base = "store " + symbol.getType() + " " + value.value2string() +
                ", " + symbol.getType() + "* ";
        if (symbol.isGlobal()) {
            return base + "@" + symbol.getName();
        } else {
            return base + symbol.getAllocInstr().value2string();
        }
    }
}
