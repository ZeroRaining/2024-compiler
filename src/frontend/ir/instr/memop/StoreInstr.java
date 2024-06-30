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
        return "store " + symbol.getType() + " " + value.value2string() +
                ", " + symbol.getType() + "* " + symbol.getAllocInstr().value2string();
    }
}
