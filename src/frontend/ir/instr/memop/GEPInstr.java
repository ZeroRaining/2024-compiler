package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;
import frontend.ir.symbols.Symbol;

import java.util.List;

/**
 * getelementptr
 * 获取指针的指令，主要用于数组操作
 * 目前的设计是无论几层都一个指令取出来，之后可能需要改成分不同情况分开或合并
 * 此外值得注意的是因为现在后端大概率是 64 位，这里的指针类型用的都是 i64
 */
public class GEPInstr extends MemoryOperation {
    private final int result;
    private final List<Value> indexList;
    
    public GEPInstr(int result, List<Value> indexList, Symbol symbol, BasicBlock parentBB) {
        super(symbol, parentBB);
        this.result = result;
        this.indexList = indexList;
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%reg_").append(result).append(" = getelementptr ");
        stringBuilder.append(symbol.printArrayTypeName()).append(", ");
        stringBuilder.append(symbol.printArrayTypeName()).append("* ");
        if (symbol.isGlobal()) {
            stringBuilder.append("@").append(symbol.getName());
        } else {
            stringBuilder.append(symbol.getAllocValue().value2string());
        }
        stringBuilder.append(", i64 0");
        for (Value index : indexList) {
            stringBuilder.append(", i64 ").append(index.value2string());
        }
        return stringBuilder.toString();
    }
    
    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }
}
