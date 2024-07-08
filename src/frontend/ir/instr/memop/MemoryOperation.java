package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.instr.Instruction;
import frontend.ir.symbols.Symbol;

public abstract class MemoryOperation extends Instruction {
    protected final Symbol symbol;
    
    public MemoryOperation(Symbol symbol, BasicBlock parentBB) {
        super(parentBB);
        if (symbol == null) {
            throw new NullPointerException();
        }
        this.symbol = symbol;
    }
    
    @Override
    public DataType getDataType() {
        return symbol.getType();
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public String printBaseType() {
        if (symbol.isArray()) {
            return symbol.printArrayTypeName();
        } else {
            return symbol.getType().toString();
        }
    }
}
