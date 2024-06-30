package frontend.ir.instr;

import frontend.ir.DataType;
import frontend.ir.symbols.Symbol;

public class LoadInstr extends Instruction {
    private final int result;
    private final Symbol symbol;
    
    public LoadInstr(int result, Symbol symbol) {
        this.result = result;
        this.symbol = symbol;
    }
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return symbol.getType();
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
