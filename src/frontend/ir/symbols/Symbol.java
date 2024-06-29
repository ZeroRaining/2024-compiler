package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.symbols.initalvalue.InitVal;
import frontend.syntax.Ast;

import java.util.List;

public class Symbol {
    private String name;
    private DataType type;
    private List<Integer> limitList;
    private boolean constant;
    private boolean global;
    private InitVal initVal;
    private Value allocInstr;   // 用来获取 IR 中保存该变量地址的指针
    
    public Symbol(String name, DataType type, List<Integer> limitList,
                  boolean constant, boolean global, InitVal initVal) {
        this.name = name;
        this.type = type;
        this. limitList = limitList;
        this.constant = constant;
        this.global = global;
        this.initVal = initVal;
    }
    
    public Symbol(boolean isGlobal, Ast.Decl decl) {
        if (decl == null) {
            throw new RuntimeException("空定义");
        }
        global = isGlobal;
        constant = decl.isConst();
        
    }
    
    public String getName() {
        return name;
    }
}