package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.binop.*;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.convop.Fp2Si;
import frontend.ir.instr.convop.Si2Fp;
import frontend.ir.instr.memop.AllocaInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.instr.unaryop.FNegInstr;
import frontend.ir.lib.Lib;
import frontend.ir.symbols.InitExpr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.Token;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Procedure {
    private final CustomList basicBlocks = new CustomList();
    private int curRegIndex = 0;
    private int curBlkIndex = 0;
    private BasicBlock curBlock;
    
    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block, SymTab funcSymTab) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        BasicBlock firstBasicBlock = new BasicBlock(curBlkIndex++);
        basicBlocks.addToTail(firstBasicBlock);
        curBlock = firstBasicBlock;
        parseCodeBlock(block, returnType, funcSymTab);
    }
    
    public void parseCodeBlock(Ast.Block block, DataType returnType, SymTab symTab) {
        if (block == null || returnType == null || curBlock == null || symTab == null) {
            throw new NullPointerException();
        }
        for (Ast.BlockItem item : block.getItems()) {
            if (item instanceof Ast.Stmt) {
                dealStmt((Ast.Stmt) item, returnType, symTab);
            } else if (item instanceof Ast.Decl) {
                dealDecl(symTab, (Ast.Decl) item);
            } else {
                throw new RuntimeException("出现了奇怪的条目");
            }
        }
    }

    public void dealStmt(Ast.Stmt item, DataType returnType, SymTab symTab) {
        if (item instanceof Ast.IfStmt) {
            dealIf((Ast.IfStmt) item, symTab);
        } else if (item instanceof Ast.Return) {
            dealReturn((Ast.Return) item, returnType, symTab);
        } else if (item instanceof Ast.Block) {
            parseCodeBlock((Ast.Block) item, returnType, new SymTab(symTab));
        } else if (item instanceof Ast.Assign) {
            dealAssign((Ast.Assign) item, symTab);
        } else if (item instanceof Ast.ExpStmt) {
            calculateExpr(((Ast.ExpStmt) item).getExp(), symTab);
        } else {
            throw new RuntimeException("出现了尚未支持的语句类型" + item.getClass());
        }
    }

    private void dealIf(Ast.IfStmt ifStmt, SymTab symTab) {
        BasicBlock bb = ((BasicBlock)basicBlocks.getTail());
        boolean hasElseBlock = ifStmt.elseStmt != null;
        BasicBlock thenBlock = new BasicBlock(curBlkIndex++);
        BasicBlock elseBlock = hasElseBlock? new BasicBlock(curBlkIndex++) : null;
        BasicBlock endBlock = new BasicBlock(curBlkIndex++);
        //TODO:确保正确计算且类型为i1
        Value cond = calculateExpr(ifStmt.condition, symTab);
        bb.addInstruction(new BranchInstr(cond,thenBlock,elseBlock,bb));
        curBlock = thenBlock;
        dealStmt(ifStmt.thenStmt, DataType.VOID, new SymTab(symTab));
        thenBlock.addInstruction(new JumpInstr(endBlock, thenBlock));
        if (hasElseBlock) {
            curBlock = elseBlock;
            dealStmt(ifStmt.thenStmt, DataType.VOID, new SymTab(symTab));
            elseBlock.addInstruction(new JumpInstr(endBlock, elseBlock));
            /* TODO：要能够切换当前的语句添加的块BB
             * TODO：要在thenblk最后加上JUMP
             * TODO：暂时没了
             */
        }
        curBlock = endBlock;
    }

//    private Value calculateCond(Ast.Exp exp, BasicBlock curblock, SymTab symTab) {
//    }

    public void dealReturn(Ast.Return item, DataType returnType, SymTab symTab) {
        if (item == null ||  returnType == null) {
            throw new NullPointerException();
        }
        Ast.Exp returnValue = (item).getReturnValue();
        
        if (returnValue == null) {
            curBlock.addInstruction(new ReturnInstr(returnType, curBlock));
        } else {
            Value value = calculateExpr(returnValue, symTab);
            assert value.getDataType() != DataType.VOID && returnType != DataType.VOID;
            if (returnType == DataType.INT && value.getDataType() == DataType.FLOAT) {
                value = new Fp2Si(curRegIndex++, value, curBlock);
                curBlock.addInstruction((Instruction) value);
            } else if (returnType == DataType.FLOAT && value.getDataType() == DataType.INT) {
                value = new Si2Fp(curRegIndex++, value, curBlock);
                curBlock.addInstruction((Instruction) value);
            }
            curBlock.addInstruction(new ReturnInstr(returnType, value, curBlock));
        }
    }
    
    private void dealAssign(Ast.Assign item, SymTab symTab) {
        if (item == null || curBlock == null || symTab == null) {
            throw new NullPointerException();
        }
        Ast.LVal lVal = item.getLVal();
        Symbol left = symTab.getSym(lVal.getName());
        Value right = calculateExpr(item.getExp(), symTab);
        if (left.getType() == DataType.FLOAT && right.getDataType() == DataType.INT) {
            right = new Si2Fp(curRegIndex++, right, curBlock);
            curBlock.addInstruction((Instruction) right);
        }
        if (left.getType() == DataType.INT && right.getDataType() == DataType.FLOAT) {
            right = new Fp2Si(curRegIndex++, right, curBlock);
            curBlock.addInstruction((Instruction) right);
        }
        curBlock.addInstruction(new StoreInstr(right, left, curBlock));
    }
    
    private void dealDecl(SymTab symTab, Ast.Decl item) {
        if (curBlock == null || symTab == null || item == null) {
            throw new NullPointerException();
        }
        symTab.addSymbols(item);
        List<Symbol> newSymList = symTab.getNewSymList();
        for (Symbol symbol : newSymList) {
            curBlock.addInstruction(new AllocaInstr(curRegIndex++, symbol, curBlock));
            Value initVal = symbol.getInitVal();
            if (initVal != null) {
                Value init;
                if (initVal instanceof ConstValue) {
                    init = initVal;
                } else if (initVal instanceof InitExpr) {
                    init = calculateExpr(((InitExpr) initVal).getExp(), symTab);
                } else {
                    throw new RuntimeException("这里还没有支持常量和表达式之外的形式");
                }
                curBlock.addInstruction(new StoreInstr(init, symbol, curBlock));
            }
        }
    }
    
    private Value calculateExpr(Ast.Exp exp, SymTab symTab) {
        if (exp == null) {
            throw new NullPointerException();
        }
        switch (exp.checkConstType(symTab)) {
            case INT:   return new ConstInt(exp.getConstInt(symTab));
            case FLOAT: return new ConstFloat(exp.getConstFloat(symTab));
        }
        if (exp instanceof Ast.BinaryExp) {
            Value firstValue = calculateExpr(((Ast.BinaryExp) exp).getFirstExp(), symTab);
            List<Ast.Exp> rest = ((Ast.BinaryExp) exp).getRestExps();
            for (int i = 0; i < rest.size(); i++) {
                Ast.Exp expI = rest.get(i);
                Token op = ((Ast.BinaryExp) exp).getOps().get(i);
                Value valueI = calculateExpr(expI, symTab);
                
                DataType type1 = firstValue.getDataType();
                DataType type2 = valueI.getDataType();
                assert type1 != DataType.VOID && type2 != DataType.VOID;
                DataType dataType = (type1 == DataType.INT && type2 == DataType.INT) ? DataType.INT : DataType.FLOAT;
                if (dataType == DataType.FLOAT) {
                    if (type1 == DataType.INT) {
                        Instruction instruction = new Si2Fp(curRegIndex++, firstValue, curBlock);
                        curBlock.addInstruction(instruction);
                        firstValue = instruction;
                    }
                    if (type2 == DataType.INT) {
                        Instruction instruction = new Si2Fp(curRegIndex++, valueI, curBlock);
                        curBlock.addInstruction(instruction);
                        valueI = instruction;
                    }
                }
                
                Instruction instruction;
                switch (op.getType()) {
                    case ADD:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FAddInstr(curRegIndex++, firstValue, valueI, curBlock);
                        } else {
                            instruction = new AddInstr(curRegIndex++, firstValue, valueI, curBlock);
                        }
                        break;
                    case SUB:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FSubInstr(curRegIndex++, firstValue, valueI, curBlock);
                        } else {
                            instruction = new SubInstr(curRegIndex++, firstValue, valueI, curBlock);
                        }
                        break;
                    case MUL:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FMulInstr(curRegIndex++, firstValue, valueI, curBlock);
                        } else {
                            instruction = new MulInstr(curRegIndex++, firstValue, valueI, curBlock);
                        }
                        break;
                    case DIV:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FDivInstr(curRegIndex++, firstValue, valueI, curBlock);
                        } else {
                            instruction = new SDivInstr(curRegIndex++, firstValue, valueI, curBlock);
                        }
                        break;
                    case MOD:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FRemInstr(curRegIndex++, firstValue, valueI, curBlock);
                        } else {
                            instruction = new SRemInstr(curRegIndex++, firstValue, valueI, curBlock);
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
                res = calculateExpr((Ast.Exp) primary, symTab);
            } else if (primary instanceof Ast.Call) {
                res = dealCall((Ast.Call) primary, symTab);
            } else if (primary instanceof Ast.LVal) {
                Symbol symbol = symTab.getSym(((Ast.LVal) primary).getName());
                if (symbol.isConstant() || symTab.isGlobal() && symbol.isGlobal()) {
                    res = symbol.getInitVal();
                } else {
                    Instruction load = new LoadInstr(curRegIndex++, symbol, curBlock);
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
                switch (res.getDataType()) {
                    case INT:
                        res = new SubInstr(curRegIndex++, new ConstInt(0), res, curBlock);
                        break;
                    case FLOAT:
                        res = new FNegInstr(curRegIndex++, res, curBlock);
                        break;
                    default:
                        throw new RuntimeException("带符号的指令不能，至少不应该连返回值都没有吧");
                }
                curBlock.addInstruction((Instruction) res);
            }
            return res;
        } else {
            throw new RuntimeException("奇怪的表达式");
        }
    }
    
    Value dealCall(Ast.Call call, SymTab symTab) {
        if (call == null) {
            throw new NullPointerException();
        }
        ArrayList<Value> rParams = new ArrayList<>();
        for (Ast.Exp exp : call.getParams()) {
            rParams.add(calculateExpr(exp, symTab));
        }
        CallInstr instr = Lib.getInstance().makeCall(curRegIndex++, call.getName(), rParams, curBlock);
        if (instr != null) {
            if (instr.getDataType() == DataType.VOID) {
                curRegIndex--;
            }
            curBlock.addInstruction(instr);
            return instr;
        } else {
            throw new RuntimeException("自定义函数调用尚未支持");
        }
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        // todo 首先应该挖个坑给参数埋起来（分配内存）
        int i = 0;
        for (CustomList.Node basicBlockNode : basicBlocks) {
            BasicBlock block = (BasicBlock) basicBlockNode;
            writer.append("blk_").append(String.valueOf(i++)).append(":\n");
            block.printIR(writer);
        }
    }
}
