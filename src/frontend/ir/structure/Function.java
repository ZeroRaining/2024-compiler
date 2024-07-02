package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;

public class Function extends Value implements FuncDef {
    private final String name;
    private final DataType returnType;
    private final Procedure procedure;
    private final SymTab symTab;
    
    public Function(Ast.FuncDef funcDef, SymTab globalSymTab) {
        if (funcDef == null) {
            throw new NullPointerException();
        }
        name = funcDef.getIdent().getContent();
        symTab = new SymTab(globalSymTab);
        switch (funcDef.getType().getType()) {
            case INT:
                returnType = DataType.INT;
                break;
            case FLOAT:
                returnType = DataType.FLOAT;
                break;
            case VOID:
                returnType = DataType.VOID;
                break;
            default:
                throw new RuntimeException("未定义的返回值类型");
        }
        procedure = new Procedure(returnType, funcDef.getFParams(), funcDef.getBody(), symTab);
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        writer.append("define ");
        switch (this.returnType) {
            case VOID:  writer.append("void ");  break;
            case FLOAT: writer.append("float "); break;
            case INT:   writer.append("i32 ");   break;
            default: throw new RuntimeException("输出时出现了未曾设想的函数类型");
        }
        writer.append("@").append(this.name);
        writer.append("(");
        // todo 这里该输出参数表了
        writer.append(") ");
        writer.append("{\n");
        this.procedure.printIR(writer);
        writer.append("}\n");
    }

    public CustomList getBasicBlocks() {
        return procedure.getBasicBlocks();
    }

    public HashSet<Symbol> getArgs(){
        return symTab.getSymbolSet();
    }

    public DataType getReturnType() {
        return returnType;
    }

    
    @Override
    public Number getValue() {
        throw new RuntimeException("函数暂时没有值");
    }
    
    @Override
    public DataType getDataType() {
        return returnType;
    }
    
    @Override
    public String value2string() {
        throw new RuntimeException("函数暂时没有值");
    }
    
    @Override
    public String getName() {
        return name;
    }
}