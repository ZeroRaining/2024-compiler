package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;
import frontend.ir.symbols.Symbol;

public class LoadInstr extends MemoryOperation {
    private final int result;
    
    public LoadInstr(int result, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.result = result;
        setUse(symbol.getAllocValue());
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%reg_").append(result).append(" = load ");
        String ty = symbol.getType().toString();
        stringBuilder.append(ty).append(", ").append(ty).append("* ");
        if (symbol.isGlobal()) {
            stringBuilder.append("@").append(symbol.getName());
        } else {
            stringBuilder.append(symbol.getAllocValue().value2string());
        }
        return stringBuilder.toString();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }

    public Symbol getSymbol() {
        return symbol;
    }
}
