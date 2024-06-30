package frontend.ir.instr.memop;

import frontend.ir.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.instr.Instruction;
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
            stringBuilder.append("%").append(symbol.getAllocInstr().getValue());
        }
        return stringBuilder.toString();
    }
}
