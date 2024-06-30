package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.syntax.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SymTab {
    private final HashMap<String, Symbol> symbolMap = new HashMap<>();
    private final SymTab parent;
    
    public SymTab(SymTab p) {
        parent = p;
    }
    
    public SymTab() {
        parent = null;
    }
    
    public boolean hasSym(String sym) {
        return symbolMap.containsKey(sym) || (parent != null && parent.hasSym(sym));
    }
    
    public Symbol getSym(String sym) {
        if (symbolMap.containsKey(sym)) {
            return symbolMap.get(sym);
        }
        if (parent == null) {
            return null;
        }
        return parent.getSym(sym);
    }
    
    public void addSym(Symbol symbol) {
        if (symbol == null) {
            throw new NullPointerException();
        }
        String name = symbol.getName();
        if (symbolMap.containsKey(name)) {
            throw new RuntimeException("重名对象");
        }
        symbolMap.put(name, symbol);
    }
    
    public void addSymbols(Ast.Decl decl) {
        if (decl == null) {
            throw new NullPointerException();
        }
        boolean constant = decl.isConst();
        DataType dataType;
        switch (decl.getType().getType()) {
            case INT:   dataType = DataType.INT;    break;
            case FLOAT: dataType = DataType.FLOAT;  break;
            default : throw new RuntimeException("出现了意料之外的声明类型");
        }
        for (Ast.Def def : decl.getDefList()) {
            assert def.getType().equals(decl.getType().getType());
            String name = def.getIdent().getContent();
            List<Integer> limList = new ArrayList<>();
            for (Ast.Exp exp : def.getIndexList()) {
                if (exp.checkConstType(this) == DataType.INT) {
                    limList.add(exp.getConstInt(this));
                } else {
                    throw new RuntimeException("数组各维长度必须是整数");
                }
            }
            Value initVal;
            Ast.Init init = def.getInit();
            if (init != null) {
                initVal = InitVal.createInitVal(dataType, init, this);
            } else {
                initVal = null;
            }
            addSym(new Symbol(name, dataType, limList, constant, parent == null, initVal));
        }
    }
    
    public boolean isGlobal() {
        return parent == null;
    }

    public HashSet<Symbol> getSymbolSet() {
        return new HashSet<>(symbolMap.values());
    }

    public List<Symbol> getAllSym() {
        return new ArrayList<>(symbolMap.values());
    }
}
