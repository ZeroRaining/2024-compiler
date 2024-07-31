package frontend.ir.instr.memop;

import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.GlobalObject;
import frontend.ir.structure.Procedure;
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
//    private final List<Value> indexList;
    private Value index;
    private final String arrayTypeName;
    private Value ptrVal;    // 指针基质：全局变量名，或者局部变量申请指令，或上一条 GEP

    public GEPInstr(int result, Value index, Symbol symbol) {
        super(symbol);
        this.result = result;
        this.index = index;
        this.pointerLevel = symbol.getDim();
        this.arrayTypeName = symbol.printArrayTypeName();
        this.ptrVal = symbol.getAllocValue();
        setUse(symbol.getAllocValue());
        if (index == null) {
            this.pointerLevel++;
        } else {
            setUse(index);
        }
    }
    
    private GEPInstr(int result, Value index, Value allocValue, Symbol symbol) {
        super(symbol);
        this.result = result;
        this.index = index;
        this.pointerLevel = symbol.getDim();
        this.arrayTypeName = symbol.printArrayTypeName();
        this.ptrVal = allocValue;
        setUse(allocValue);
        if (index == null) {
            this.pointerLevel++;
        } else {
            setUse(index);
        }
    }

    public GEPInstr(int result, Value index, GEPInstr base) {
        super(base.symbol);
        this.result = result;
        this.index = index;
        this.pointerLevel = base.pointerLevel - 1;
        this.arrayTypeName = base.printBaseType();
        this.ptrVal = base;
        setUse(base);
        if (index == null) {
            throw new RuntimeException("已经取过一次指针了，不应该再没有 index 了");
        } else {
            setUse(index);
        }
    }

    public GEPInstr(int result, LoadInstr base, Value index) {
        super(base.symbol);
        this.result = result;
        this.index = index;
        this.pointerLevel = symbol.getDim();
        String superType = base.printBaseType();
        this.arrayTypeName = superType.substring(0, superType.length() - 1);
        this.ptrVal = base;
        setUse(base);
        if (index == null) {
            throw new RuntimeException("不应该再没有 index 了");
        } else {
            setUse(index);
        }
    }
    
    @Override
    public Instruction cloneShell(Procedure procedure) {
        if (ptrVal instanceof GlobalObject || ptrVal instanceof AllocaInstr) {
            return new GEPInstr(procedure.getAndAddRegIndex(), index, ptrVal, symbol);
        } else if (ptrVal instanceof GEPInstr) {
            return new GEPInstr(procedure.getAndAddRegIndex(), index, (GEPInstr) ptrVal);
        } else if (ptrVal instanceof LoadInstr) {
            return new GEPInstr(procedure.getAndAddRegIndex(), (LoadInstr) ptrVal, index);
        } else {
            throw new RuntimeException("GEP 的指针基址还有什么其它可能吗？");
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
        return "%reg_" + result + " = getelementptr " +
                printTypeAndIndex();
    }
    
    private String printTypeAndIndex() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(arrayTypeName).append(", ");
        stringBuilder.append(arrayTypeName).append("* ");
        stringBuilder.append(ptrVal.value2string());
        if (!(symbol.isArrayFParam() && ptrVal instanceof LoadInstr)) {
            stringBuilder.append(", i64 0");
        }
        if (index != null) {
            stringBuilder.append(", i64 ").append(index.value2string());
        }
        return stringBuilder.toString();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (this.ptrVal == from) {
            this.ptrVal = to;
        } else if (index == from) {
            index = to;
        } else {
            throw new RuntimeException("没有可以置换的 value");
        }
    }

    public List<Value> getWholeIndexList() {
        return null;//todo
//        List<Value> wholeIndexList = new ArrayList<>(indexList);
//        for (int i = 0; i < symbol.getLimitList().size() - indexList.size(); i++) {
//            wholeIndexList.add(ConstInt.Zero);
//        }
//        return wholeIndexList;
    }

    @Override
    public String myHash() {
        return this.printTypeAndIndex();
    }
}
