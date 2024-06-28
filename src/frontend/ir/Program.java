package frontend.ir;

import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

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
                functions.put(funcName, new Function((Ast.FuncDef) compUnit));
            } else if (compUnit instanceof Ast.Decl) {
                globalSymTab.addSymbols(true, (Ast.Decl) compUnit);
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
        for (Function function : functions.values()) {
            function.printIR(writer);
        }
    }
}
