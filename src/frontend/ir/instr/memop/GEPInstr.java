package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.symbols.Symbol;

import java.util.ArrayList;
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
    private final String arrayTypeName;
    private Value ptrVal;    // 指针基质，全局变量名，或者局部变量申请指令，或上一条 GEP

    public GEPInstr(int result, List<Value> indexList, Symbol symbol) {
        super(symbol);
        if (indexList == null) {
            throw new NullPointerException();
        }
        this.result = result;
        this.indexList = indexList;
        this.pointerLevel = symbol.getDim() + 1 - indexList.size();
        this.arrayTypeName = symbol.printArrayTypeName();
        this.ptrVal = symbol.getAllocValue();
        setUse(symbol.getAllocValue());
        for (Value value : indexList) {
            setUse(value);
        }
    }

    public GEPInstr(int result, GEPInstr base) {
        super(base.symbol);
        this.result = result;
        this.indexList = new ArrayList<>();
        indexList.add(new ConstInt(0));
        this.pointerLevel = base.pointerLevel - 1;
        this.arrayTypeName = base.printBaseType();
        this.ptrVal = base;
        setUse(base);
    }

    public GEPInstr(int result, LoadInstr base, List<Value> indexList) {
        super(base.symbol);
        this.result = result;
        this.indexList = indexList;
        this.pointerLevel = symbol.getDim() + 1 - indexList.size();
        String superType = base.printBaseType();
        this.arrayTypeName = superType.substring(0, superType.length() - 1);
        this.ptrVal = base;
        setUse(base);
        for (Value value : indexList) {
            setUse(value);
        }
    }

    @Override
    public String type2string() {
        return this.printBaseType() + "*";
    }

    @Override
    public String printBaseType() {
        if (this.pointerLevel > 1) {
            List<Integer> limList = this.symbol.getLimitList();
            StringBuilder stringBuilder = new StringBuilder();
            int lim = limList.size();
            int start = lim + 1 - pointerLevel;
            for (int i = start; i < lim; i++) {
                stringBuilder.append("[").append(limList.get(i)).append(" x ");
            }
            stringBuilder.append(symbol.getType());
            for (int i = start; i < lim; i++) {
                stringBuilder.append("]");
            }
            return stringBuilder.toString();
        } else {
            return this.symbol.getType().toString();
        }
    }

    public List<Integer> getSizeList() {
        List<Integer> sizeList = new ArrayList<>();
        sizeList.add(1);
        int size = symbol.getLimitList().size();
        for (int i = size - 1; i >= 0; i--) {
            sizeList.add(0, sizeList.get(0) * symbol.getLimitList().get(i));
        }
        sizeList.remove(0);
        return sizeList;
    }

    public Value getPtrVal() {
        return ptrVal;
    }

    @Override
    public Number getNumber() {
        return result;
    }

    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%reg_").append(result).append(" = getelementptr ");
        stringBuilder.append(arrayTypeName).append(", ");
        stringBuilder.append(arrayTypeName).append("* ");
        stringBuilder.append(ptrVal.value2string());
        if (!symbol.isArrayFParam()) {
            stringBuilder.append(", i64 0");
        }
        for (Value index : indexList) {
            stringBuilder.append(", i64 ").append(index.value2string());
        }
        return stringBuilder.toString();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (this.ptrVal == from) {
            this.ptrVal = to;
        } else {
            for (int i = 0; i < indexList.size(); i++) {
                if (indexList.get(i) == from) {
                    indexList.set(i, to);
                    return;
                }
            }
            throw new RuntimeException("没有可以置换的 value");
        }
    }

    public List<Value> getWholeIndexList() {
        List<Value> wholeIndexList = new ArrayList<>();
        wholeIndexList.addAll(indexList);
        for (int i = 0; i < symbol.getLimitList().size() - indexList.size(); i++) {
            wholeIndexList.add(new ConstInt(0));
        }
        return wholeIndexList;
    }

    @Override
    public String myHash() {
        return this.value2string();
    }
}
