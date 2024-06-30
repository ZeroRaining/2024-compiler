package frontend.ir;

import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.instr.AddInstr;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.LoadInstr;
import frontend.ir.instr.ReturnInstr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.Token;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Procedure {
    private final ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
    private final SymTab symTab;
    private int curRegIndex = 0;
    
    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block, SymTab baseSymTab) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        basicBlocks.add(new BasicBlock());
        this.symTab = baseSymTab;
        
        for (Ast.BlockItem item : block.getItems()) {
            if (item instanceof Ast.Stmt) {
                if (item instanceof Ast.Return) {
                    Ast.Exp returnValue = ((Ast.Return) item).getReturnValue();
                    if (returnValue == null) {
                        basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType));
                    } else {
                        Value value;
                        switch (returnValue.checkConstType(symTab)) {
                            case INT:
                                value = new ConstInt(returnValue.getConstInt(symTab));
                                break;
                            case FLOAT:
                                value = new ConstFloat(returnValue.getConstFloat(symTab));
                                break;
                            default:
                                // 返回值非常量
                                value = calculateExpr(returnValue);
                                assert value instanceof Instruction;
                        }
                        Instruction ret = new ReturnInstr(returnType, value);
                        basicBlocks.get(basicBlocks.size() - 1).addInstruction(ret);
                    }
                    
                }
            } else if (item instanceof Ast.Decl) {
            
            } else {
                throw new RuntimeException("出现了奇怪的条目");
            }
        }
    }
    
    private Value calculateExpr(Ast.Exp exp) {
        BasicBlock curBlock = basicBlocks.get(basicBlocks.size() - 1);
        if (exp instanceof Ast.BinaryExp) {
            Value firstValue = calculateExpr(((Ast.BinaryExp) exp).getFirstExp());
            List<Ast.Exp> rest = ((Ast.BinaryExp) exp).getRestExps();
            for (int i = 0; i < rest.size(); i++) {
                Ast.Exp expI = rest.get(i);
                Token op = ((Ast.BinaryExp) exp).getOps().get(i);
                Value valueI = calculateExpr(expI);
                Instruction instruction;
                switch (op.getType()) {
                    // todo 常量和变量（用 instr 存统一到 value 下面
                    case ADD: instruction = new AddInstr(curRegIndex++, firstValue, valueI); break;
//                    case SUB: res -= num; break;
//                    case MUL: res *= num; break;
//                    case DIV: res /= num; break;
//                    case MOD: res %= num; break;
                    default: throw new RuntimeException("表达式计算过程中出现了未曾设想的运算符");
                }
                curBlock.addInstruction(instruction);
                firstValue = instruction;
            }
            return firstValue;
        } else if (exp instanceof Ast.UnaryExp) {
            int sign = ((Ast.UnaryExp) exp).getSign();
            Ast.PrimaryExp primary = ((Ast.UnaryExp) exp).getPrimaryExp();
            Value res;
            if (primary instanceof Ast.Exp) {
                res = calculateExpr((Ast.Exp) primary);
            } else if (primary instanceof Ast.Call) {
                // todo
                return null;
            } else if (primary instanceof Ast.LVal) {
                Symbol symbol = symTab.getSym(((Ast.LVal) primary).getName());
                if (symbol.isConstant() || symTab.isGlobal() && symbol.isGlobal()) {
                    res = symbol.getInitVal();
                } else {
                    Instruction load = new LoadInstr(curRegIndex++, symbol);
                    curBlock.addInstruction(load);
                    res = load;
                }
            } else if (primary instanceof Ast.Number) {
                if (((Ast.Number) primary).isIntConst()) {
                    res = new ConstInt(((Ast.Number) primary).getIntConstValue() * sign);
                    sign = 1;
                } else if (((Ast.Number) primary).isFloatConst()) {
                    res = new ConstFloat(((Ast.Number) primary).getFloatConstValue() * sign);
                    sign = 1;
                } else {
                    throw new RuntimeException("出现了未定义的数值常量类型");
                }
            } else {
                throw new RuntimeException("出现了未定义的基本表达式");
            }
            assert sign == 1 || sign == -1;
            if (sign == -1) {
                // todo 加一条减法指令
                // res = 上一条
            }
            return res;
        } else {
            throw new RuntimeException("奇怪的表达式");
        }
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        // todo 首先应该挖个坑给参数埋起来（分配内存）
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);
            writer.append("blk_").append(Integer.toString(i)).append(":\n");
            block.printIR(writer);
        }
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }
}
