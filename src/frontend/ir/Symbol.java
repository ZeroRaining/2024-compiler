package frontend.ir;

import frontend.syntax.Ast;

import java.util.List;

public class Symbol {
    private String name;
    private DataType type;
    private List<Integer> limitList;
    private boolean constant;
    private boolean global;
    
    public Symbol(String name, DataType type, List<Integer> limitList, boolean constant, boolean global) {
        this.name = name;
        this.type = type;
        this. limitList = limitList;
        this.constant = constant;
        this.global = global;
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
