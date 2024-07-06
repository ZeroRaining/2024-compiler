package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
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
    
    public List<Symbol> parseNewSymbols(Ast.Decl decl) {
        ArrayList<Symbol> newSymList = new ArrayList<>();
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
                initVal = createInitVal(dataType, init, limList);
            } else if (!limList.isEmpty()) {
                initVal = new ArrayInitVal(dataType, limList);
            } else if (isGlobal()) {
                initVal = dataType == DataType.FLOAT ? new ConstFloat(0.0f) :
                                                       new ConstInt(0);
            } else {
                initVal = null;
            }
            Symbol symbol = new Symbol(name, dataType, limList, constant, parent == null, initVal);
            newSymList.add(symbol);
        }
        return newSymList;
    }
    
    private Value createInitVal(DataType type, Ast.Init init, List<Integer> limList) {
        if (init instanceof Ast.Exp) {
            return dealNonArrayInit(type, (Ast.Exp) init);
        } else if (init instanceof Ast.InitArray) {
            return dealArrayInit(type, (Ast.InitArray) init, limList);
        } else {
            throw new RuntimeException("奇怪的定义类型");
        }
    }
    
    private Value dealNonArrayInit(DataType type, Ast.Exp init) {
        if (type == null || init == null) {
            throw new NullPointerException();
        }
        switch (init.checkConstType(this)) {
            case INT:
                if (type == DataType.INT) {
                    return new ConstInt(init.getConstInt(this));
                } else if (type == DataType.FLOAT) {
                    return new ConstFloat(init.getConstInt(this).floatValue());
                }
                else {
                    throw new RuntimeException("你给我传了个什么鬼类型啊");
                }
            case FLOAT:
                if (type == DataType.INT) {
                    return new ConstInt(init.getConstFloat(this).intValue());
                } else if (type == DataType.FLOAT) {
                    return new ConstFloat(init.getConstFloat(this));
                } else {
                    throw new RuntimeException("你给我传了个什么鬼类型啊");
                }
            default:
                if (this.isGlobal()) {
                    throw new RuntimeException("全局变量初始值似乎不是确定值");
                }
                return new InitExpr(init);
        }
    }
    
    private Value dealArrayInit(DataType type, Ast.InitArray init, List<Integer> limList) {
        if (type == null || init == null) {
            throw new NullPointerException();
        }
        List<Ast.Init> initList = init.getInitList();
        ArrayInitVal arrayInitVal = new ArrayInitVal(type, limList);
        if (limList.size() > 1) {
            int len = limList.get(limList.size() - 1);
            int cnt = 0;
            List<Integer> nextLimList = new ArrayList<>(limList.subList(1, limList.size()));
            ArrayInitVal innerInitVal = new ArrayInitVal(type, nextLimList);
            for (Ast.Init innerInit : initList) {
                if (innerInit instanceof Ast.Exp) {
                    if (cnt++ >= len) {
                        cnt = 1;
                        arrayInitVal.addInitValue(innerInitVal);
                        innerInitVal = new ArrayInitVal(type, nextLimList);
                    }
                    innerInitVal.addInitValue(dealNonArrayInit(type, (Ast.Exp) innerInit));
                } else if (innerInit instanceof Ast.InitArray) {
                    if (cnt > 0) {
                        arrayInitVal.addInitValue(innerInitVal);
                        innerInitVal = new ArrayInitVal(type, nextLimList);
                        cnt = 0;
                    }
                    arrayInitVal.addInitValue(dealArrayInit(type, (Ast.InitArray) innerInit, nextLimList));
                } else {
                    throw new RuntimeException("奇怪的定义类型");
                }
            }
            if (cnt > 0) {
                arrayInitVal.addInitValue(innerInitVal);
            }
            
        } else {
            for (Ast.Init innerInit : initList) {
                Value initVal;
                if (innerInit instanceof Ast.Exp) {
                    initVal = dealNonArrayInit(type, (Ast.Exp) innerInit);
                } else {
                    throw new RuntimeException("最低维度只应该出现非数组类型");
                }
                arrayInitVal.addInitValue(initVal);
            }
        }
        
        return arrayInitVal;
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
