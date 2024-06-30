package frontend.ir;

import Utils.CustomList;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.binop.*;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.convop.Fp2Si;
import frontend.ir.instr.convop.Si2Fp;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.Token;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class Procedure {
    private final CustomList<BasicBlock> basicBlocks = new CustomList<>();
    private final SymTab symTab;
    private int curRegIndex = 0;
    
    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block, SymTab baseSymTab) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        basicBlocks.addToTail(new BasicBlock());
        this.symTab = baseSymTab;
        
        for (Ast.BlockItem item : block.getItems()) {
            if (item instanceof Ast.Stmt) {
                if (item instanceof Ast.Return) {
                    Ast.Exp returnValue = ((Ast.Return) item).getReturnValue();
                    BasicBlock bb = ((BasicBlock)basicBlocks.getTail());
                    if (returnValue == null) {
                        bb.addInstruction(new ReturnInstr(returnType));
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

                        assert value.getDataType() != DataType.VOID && returnType != DataType.VOID;
                        if (returnType == DataType.INT && value.getDataType() == DataType.FLOAT) {
                            value = new Fp2Si(curRegIndex++, value);
                            bb.addInstruction((Instruction) value);
                        } else if (returnType == DataType.FLOAT && value.getDataType() == DataType.INT) {
                            value = new Si2Fp(curRegIndex++, value);
                            bb.addInstruction((Instruction) value);
                        }
                        bb.addInstruction(new ReturnInstr(returnType, value));
                        
                    }
                    
                }
            } else if (item instanceof Ast.Decl) {
            
            } else {
                throw new RuntimeException("出现了奇怪的条目");
            }
        }
    }
    
    private Value calculateExpr(Ast.Exp exp) {
        BasicBlock curBlock = (BasicBlock) basicBlocks.getTail();
        if (exp instanceof Ast.BinaryExp) {
            Value firstValue = calculateExpr(((Ast.BinaryExp) exp).getFirstExp());
            List<Ast.Exp> rest = ((Ast.BinaryExp) exp).getRestExps();
            for (int i = 0; i < rest.size(); i++) {
                Ast.Exp expI = rest.get(i);
                Token op = ((Ast.BinaryExp) exp).getOps().get(i);
                Value valueI = calculateExpr(expI);
                
                DataType type1 = firstValue.getDataType();
                DataType type2 = valueI.getDataType();
                assert type1 != DataType.VOID && type2 != DataType.VOID;
                DataType dataType = (type1 == DataType.INT && type2 == DataType.INT) ? DataType.INT : DataType.FLOAT;
                if (dataType == DataType.FLOAT) {
                    if (type1 == DataType.INT) {
                        Instruction instruction = new Si2Fp(curRegIndex++, firstValue);
                        curBlock.addInstruction(instruction);
                        firstValue = instruction;
                    }
                    if (type2 == DataType.INT) {
                        Instruction instruction = new Si2Fp(curRegIndex++, valueI);
                        curBlock.addInstruction(instruction);
                        valueI = instruction;
                    }
                }
                
                Instruction instruction;
                switch (op.getType()) {
                    case ADD:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FAddInstr(curRegIndex++, firstValue, valueI);
                        } else {
                            instruction = new AddInstr(curRegIndex++, firstValue, valueI);
                        }
                        break;
                    case SUB:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FSubInstr(curRegIndex++, firstValue, valueI);
                        } else {
                            instruction = new SubInstr(curRegIndex++, firstValue, valueI);
                        }
                        break;
                    case MUL:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FMulInstr(curRegIndex++, firstValue, valueI);
                        } else {
                            instruction = new MulInstr(curRegIndex++, firstValue, valueI);
                        }
                        break;
                    case DIV:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FDivInstr(curRegIndex++, firstValue, valueI);
                        } else {
                            instruction = new SDivInstr(curRegIndex++, firstValue, valueI);
                        }
                        break;
                    case MOD:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FRemInstr(curRegIndex++, firstValue, valueI);
                        } else {
                            instruction = new SRemInstr(curRegIndex++, firstValue, valueI);
                        }
                        break;
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
                throw new RuntimeException("还没写到");
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
        int i = 0;
        for (CustomList.Node<BasicBlock> basicBlockNode : basicBlocks) {
            BasicBlock block = (BasicBlock) basicBlockNode;
            writer.append("blk_").append(String.valueOf(i++)).append(":\n");
            block.printIR(writer);
        }
    }
}
