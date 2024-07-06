package frontend.ir.structure;

import frontend.ir.lib.Lib;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Program {
    private final SymTab globalSymTab = new SymTab();
    private final HashMap<String, Function> functions = new HashMap<>();
    
    public Program(Ast ast) {
        if (ast == null) {
            throw new NullPointerException();
        }
        for (Ast.CompUnit compUnit : ast.getUnits()) {
            if (compUnit instanceof Ast.FuncDef) {
                String funcName = ((Ast.FuncDef) compUnit).getIdent().getContent();
                if (functions.containsKey(funcName)) {
                    throw new RuntimeException("重复的函数命名");
                }
                if (globalSymTab.hasSym(funcName)) {
                    throw new RuntimeException("函数命名与全局变量名重复");
                }
                functions.put(funcName, new Function((Ast.FuncDef) compUnit, globalSymTab));
            } else if (compUnit instanceof Ast.Decl) {
                List<Symbol> newSymList = globalSymTab.parseNewSymbols((Ast.Decl) compUnit);
                for (Symbol symbol : newSymList) {
                    globalSymTab.addSym(symbol);
                }
            } else {
                throw new RuntimeException("未定义的编译单元");
            }
        }
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        writer.write("");
        writeGlobalDecl(writer);
        Lib.getInstance().declareUsedFunc(writer);
        int i = 0;
        for (Function function : functions.values()) {
            function.printIR(writer);
            if (++i < functions.size()) {
                writer.append("\n");
            }
        }
    }

    public List<Symbol> getGlobalVars() {
        return globalSymTab.getSymbolList();
    }
    public HashMap<String, Function> getFunctions(){
        return functions;
    }

    private void writeGlobalDecl(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        if (globalSymTab.getAllSym().isEmpty()) {
            return;
        }
        for (Symbol symbol : globalSymTab.getAllSym()) {
            writer.append("@").append(symbol.getName()).append(" = global ");
            if (symbol.isArray()) {
                writer.append(symbol.printArrayTypeName());
            } else {
                writer.append(symbol.getType().toString());
            }
            writer.append(" ");
            writer.append(symbol.getInitVal().value2string()).append("\n");
        }
        writer.append("\n");
    }
}
