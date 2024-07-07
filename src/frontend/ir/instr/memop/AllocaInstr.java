package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;
import frontend.ir.symbols.Symbol;

public class AllocaInstr extends MemoryOperation {
    private final int result;
    
    public AllocaInstr(int result, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.result = result;
        symbol.setAllocValue(this);
        this.pointerLevel = symbol.getDim() + 1;
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public String print() {
        if (symbol.isArray()) {
            return "%reg_" + result + " = alloca " + symbol.printArrayTypeName();
        } else {
            return "%reg_" + result + " = alloca " + getDataType();
        }
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }
}
