package frontend.ir.symbols;

import backend.itemStructure.AsmType;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.structure.GlobalObject;

import java.util.List;

public class Symbol {
    private final String name;
    private final DataType type;
    private final List<Integer> limitList;
    private final boolean constant;
    private final boolean global;
    private final Value initVal;
    private Value allocValue;   // 用来获取 IR 中保存该变量地址的指针

    public Symbol(String name, DataType type, List<Integer> limitList,
                  boolean constant, boolean global, Value initVal) {
        this.name = name;
        this.type = type;
        this.limitList = limitList;
        this.constant = constant;
        this.global = global;
        this.initVal = initVal;
        if (global) {
            this.allocValue = new GlobalObject(name, type);
        } else {
            this.allocValue = null;
        }
    }

    public String getName() {
        return name;
    }

    public boolean isGlobal() {
        return global;
    }

    public boolean isConstant() {
        return constant;
    }

    public DataType getType() {
        return type;
    }

    public Value getInitVal() {
        return initVal;
    }

    public void setAllocValue(Value allocValue) {
        this.allocValue = allocValue;
    }
    
    public List<Integer> getLimitList() {
        return limitList;
    }
    
    public boolean isArray() {
        return !limitList.isEmpty();
    }

    public AsmType getAsmType() {
        if (!(limitList == null || limitList.isEmpty())) {
            return AsmType.ARRAY;
        }
        if (type == DataType.INT) {
            return AsmType.INT;
        } else if (type == DataType.FLOAT) {
            return AsmType.FLOAT;
        } else {
            throw new RuntimeException("变量出现void类型");
        }
    }

    public Number getValue() {
        return initVal.getNumber();
    }

    public Value getAllocValue() {
        if (allocValue == null) {
            throw new RuntimeException("还没有定义");
        }
        return allocValue;
    }
    
    public String printArrayTypeName() {
        if (initVal instanceof ArrayInitVal) {
            return ((ArrayInitVal) initVal).printArrayTypeName();
        }
        throw new RuntimeException("这是数组吗？");
    }
}
