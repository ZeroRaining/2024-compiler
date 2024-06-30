package frontend.ir.instr.memop;

import frontend.ir.BasicBlock;
import frontend.ir.symbols.Symbol;

public class AllocaInstr extends MemoryOperation {
    private final int result;
    
    public AllocaInstr(int result, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.result = result;
        symbol.setAllocInstr(this);
    }
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public String print() {
        return "%" + result + " = alloca " + getDataType();
    }
}
