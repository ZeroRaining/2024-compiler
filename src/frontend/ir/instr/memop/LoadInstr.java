package frontend.ir.instr.memop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.symbols.Symbol;

public class LoadInstr extends MemoryOperation {
    private final int result;
    
    public LoadInstr(int result, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.result = result;
    }
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%").append(result).append(" = load ");
        String ty = symbol.getType().toString();
        stringBuilder.append(ty).append(", ").append(ty).append("* ");
        if (symbol.isGlobal()) {
            stringBuilder.append("@").append(symbol.getName());
        } else {
            stringBuilder.append(symbol.getAllocInstr().value2string());
        }
        return stringBuilder.toString();
    }
    public Symbol getSymbol() {
        return symbol;
    }
}
