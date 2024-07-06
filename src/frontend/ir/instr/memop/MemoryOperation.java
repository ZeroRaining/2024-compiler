package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.instr.Instruction;
import frontend.ir.symbols.Symbol;

public abstract class MemoryOperation extends Instruction {
    protected final Symbol symbol;
    protected final DataType type;
    
    public MemoryOperation(Symbol symbol, BasicBlock parentBB) {
        super(parentBB);
        if (symbol == null) {
            throw new NullPointerException();
        }
        this.symbol = symbol;
        this.type = symbol.getType();
    }
    
    @Override
    public DataType getDataType() {
        return type;
    }
    private Symbol getSymbol() {
        return symbol;
    }
}
