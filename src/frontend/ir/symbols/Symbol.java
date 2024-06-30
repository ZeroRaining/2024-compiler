package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.syntax.Ast;

import java.util.List;

public class Symbol {
    private final String name;
    private final DataType type;
    private final List<Integer> limitList;
    private final boolean constant;
    private final boolean global;
    private final Value initVal;
    private Value allocInstr;   // 用来获取 IR 中保存该变量地址的指针
    
    public Symbol(String name, DataType type, List<Integer> limitList,
                  boolean constant, boolean global, Value initVal) {
        this.name = name;
        this.type = type;
        this. limitList = limitList;
        this.constant = constant;
        this.global = global;
        this.initVal = initVal;
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
    
    public void setAllocInstr(Value allocInstr) {
        this.allocInstr = allocInstr;
    }
    
    public Value getAllocInstr() {
        return allocInstr;
    }
}
