package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.syntax.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymTab {
    private final HashMap<String, Symbol> symbolMap = new HashMap<>();
    private final SymTab parent;
    private final ArrayList<Symbol> newSymList = new ArrayList<>();
        // 每次新加一系列 symbol 的时候更新，用来记录最新一批，用于给临时变量申请空间
    
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
        newSymList.clear();
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
            } else if (isGlobal()) {
                initVal = dataType == DataType.FLOAT ? new ConstFloat(0.0f) :
                                                       new ConstInt(0);
            } else {
                initVal = null;
            }
            Symbol symbol = new Symbol(name, dataType, limList, constant, parent == null, initVal);
            newSymList.add(symbol);
            addSym(symbol);
        }
    }
    
    public List<Symbol> getNewSymList() {
        return newSymList;
    }
    
    public boolean isGlobal() {
        return parent == null;
    }
    
    public List<Symbol> getAllSym() {
        return new ArrayList<>(symbolMap.values());
    }
}
