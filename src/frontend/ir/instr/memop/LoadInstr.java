package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.symbols.Symbol;

public class LoadInstr extends MemoryOperation {
    private final int result;
    private Value ptr = null;
    
    public LoadInstr(int result, Symbol symbol) {
        super(symbol);
        this.result = result;
        setUse(symbol.getAllocValue());
        this.pointerLevel = symbol.getAllocValue().getPointerLevel() - 1;
    }
    
    public LoadInstr(int result, Symbol symbol, Value ptr) {
        super(symbol);
        this.result = result;
        this.ptr = ptr;
        setUse(ptr);
        this.pointerLevel = ptr.getPointerLevel() - 1;
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%reg_").append(result).append(" = load ");
        String ty;
        if (ptr != null && ptr.getPointerLevel() == 1) {
            ty = symbol.getType().toString();
        } else {
            ty = printBaseType();
        }
        stringBuilder.append(ty).append(", ").append(ty).append("* ");
        if (ptr != null) {
            stringBuilder.append(ptr.value2string());
        } else if (symbol.isGlobal()) {
            stringBuilder.append("@").append(symbol.getName());
        } else {
            stringBuilder.append(symbol.getAllocValue().value2string());
        }
        return stringBuilder.toString();
    }
    
    @Override
    public String type2string() {
        if (this.pointerLevel > 0) {
            return this.printBaseType();
        } else {
            return this.getDataType().toString();
        }
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }
    
    public Value getPtr() {
        return ptr;
    }
    
    @Override
    public String myHash() {
        return this.value2string();
    }
}
