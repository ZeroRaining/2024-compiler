package frontend.ir;

import frontend.ir.instr.ReturnInstr;
import frontend.ir.symbols.SymTab;
import frontend.lexer.Token;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Procedure {
    private final ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
    private final SymTab symTab;
    private int curRegIndex;
    
    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block, SymTab baseSymTab) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        basicBlocks.add(new BasicBlock());
        this.symTab = baseSymTab;
        curRegIndex = 0;
        
        for (Ast.BlockItem item : block.getItems()) {
            if (item instanceof Ast.Stmt) {
                if (item instanceof Ast.Return) {
                    Ast.Exp returnValue = ((Ast.Return) item).getReturnValue();
                    if (returnValue == null) {
                        basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType));
                    } else {
                        switch (returnValue.checkConstType(symTab)) {
                            case INT:
                                Integer retI = returnValue.getConstInt(symTab);
                                assert retI != null;
                                basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType, retI));
                                break;
                            case FLOAT:
                                Float retF = returnValue.getConstFloat(symTab);
                                assert retF != null;
                                basicBlocks.get(basicBlocks.size() - 1).addInstruction(new ReturnInstr(returnType, retF));
                                break;
                            default:
                                // 返回值非常量
                                // todo
                        }
                    }
                    
                }
            } else if (item instanceof Ast.Decl) {
            
            } else {
                throw new RuntimeException("出现了奇怪的条目");
            }
        }
    }
    
    private Value calculateExpr(Ast.Exp exp) {
        if (exp instanceof Ast.BinaryExp) {
            Value firstValue = calculateExpr(((Ast.BinaryExp) exp).getFirstExp());
            List<Ast.Exp> rest = ((Ast.BinaryExp) exp).getRestExps();
            for (int i = 0; i < rest.size(); i++) {
                Ast.Exp expI = rest.get(i);
                Token op = ((Ast.BinaryExp) exp).getOps().get(i);
                Value valueI = calculateExpr(expI);
                switch (op.getType()) {
//                    case ADD: res += num; break;
//                    case SUB: res -= num; break;
//                    case MUL: res *= num; break;
//                    case DIV: res /= num; break;
//                    case MOD: res %= num; break;
                    default: throw new RuntimeException("表达式计算过程中出现了未曾设想的运算符");
                }
            }
        }
        return null;
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
}
