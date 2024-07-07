package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.TokenType;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Function extends Value implements FuncDef {
    private static final HashMap<String, Function> FUNCTION_MAP = new HashMap<>();
    private final String name;
    private final DataType returnType;
    private final Procedure procedure;
    private final SymTab symTab;
    private final List<Ast.FuncFParam> fParams;

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
        fParams = funcDef.getFParams();
        FUNCTION_MAP.put(name, this);
        procedure = new Procedure(returnType, fParams, funcDef.getBody(), symTab);
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
        printFParams(writer);
        writer.append(") ");
        writer.append("{\n");
        this.procedure.printIR(writer);
        writer.append("}\n");
    }

    private void printFParams(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < fParams.size(); i++) {
            Ast.FuncFParam param = fParams.get(i);
            if (param.isArray()) {
                // todo
                throw new RuntimeException("暂时不能处理数组");
            } else {
                switch (param.getType().getType()) {
                    case INT:   writer.append("i32");   break;
                    case FLOAT: writer.append("float"); break;
                    default: throw new RuntimeException("出现了奇怪的形参类型");
                }
                writer.append(" %").append(Integer.toString(i));
            }
            if (i < fParams.size() - 1) {
                writer.append(", ");
            }
        }
    }

    public CustomList getBasicBlocks() {
        return procedure.getBasicBlocks();
    }

    public List<Symbol> getArgs(){
        return symTab.getSymbolList();
    }

    public static CallInstr makeCall(int result, String name, List<Value> rParams, BasicBlock curBlock) {
        if (rParams == null || name == null || curBlock == null) {
            throw new NullPointerException();
        }
        Function function = FUNCTION_MAP.get(name);
        if (function == null) {
            return null;
        }
        if (!function.checkParams(rParams)) {
            throw new RuntimeException("形参实参不匹配");
        }
        DataType type = function.getDataType();
        if (type == DataType.VOID) {
            return new CallInstr(null, type, function, rParams, curBlock);
        } else {
            return new CallInstr(result, type, function, rParams, curBlock);
        }
    }

    private boolean checkParams(List<Value> rParams) {
        if (rParams == null) {
            throw new NullPointerException();
        }
        if (rParams.size() != fParams.size()) {
            return false;
        }
        for (int i = 0; i < fParams.size(); i++) {
            if (rParams.get(i).getDataType() == DataType.INT &&
                    fParams.get(i).getType().getType() != TokenType.INT) {
                return false;
            }
            if (rParams.get(i).getDataType() == DataType.FLOAT &&
                    fParams.get(i).getType().getType() != TokenType.FLOAT) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Number getNumber() {
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
