package frontend.ir.structure;

import Utils.CustomList;
import debug.DEBUG;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.binop.*;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.convop.*;
import frontend.ir.instr.memop.AllocaInstr;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.otherop.cmp.CmpCond;
import frontend.ir.instr.otherop.cmp.FCmpInstr;
import frontend.ir.instr.otherop.cmp.ICmpInstr;
import frontend.ir.instr.terminator.BranchInstr;
import frontend.ir.instr.terminator.JumpInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.instr.unaryop.FNegInstr;
import frontend.ir.lib.Lib;
import frontend.ir.lib.LibFunc;
import frontend.ir.symbols.ArrayInitVal;
import frontend.ir.symbols.InitExpr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.Token;
import frontend.lexer.TokenType;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Procedure {
    private final CustomList basicBlocks = new CustomList();
    private int curRegIndex = 0;
    private int curBlkIndex = 0;
    private int curDepth = 0;
    private BasicBlock curBlock;
    private final BasicBlock retBlock;
    private final Stack<BasicBlock> whileBegins;
    private final Stack<BasicBlock> whileEnds;
    private final ArrayList<Value> fParamValueList = new ArrayList<>();

    public Procedure(DataType returnType, List<Ast.FuncFParam> fParams, Ast.Block block, SymTab funcSymTab) {
        if (fParams == null || block == null) {
            throw new NullPointerException();
        }
        BasicBlock firstBasicBlock = new BasicBlock(curDepth);
        firstBasicBlock.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(firstBasicBlock);
        curBlock = firstBasicBlock;
        retBlock = new BasicBlock(curDepth);
        whileBegins = new Stack<>();
        whileEnds = new Stack<>();
        HashMap<Symbol, FParam> symbol2FParam = new HashMap<>();
        parseParams(fParams, funcSymTab, symbol2FParam);
        init(returnType, funcSymTab);
        storeParams(symbol2FParam);
        parseCodeBlock(block, returnType, funcSymTab);
        finalize(returnType, funcSymTab);
    }

    private void finalize(DataType returnType, SymTab funcSymTab) {
        if (returnType == null) {
            throw new RuntimeException("会不会写函数啊，小老弟？！");
        }
        if (returnType != DataType.VOID) {
            //空串表示用于存返回值的变量
            curBlock = retBlock;
            curBlock.setLabelCnt(curBlkIndex++);
            basicBlocks.addToTail(curBlock);
            Instruction instr = new LoadInstr(curRegIndex++,funcSymTab.getSym(""));
            curBlock.addInstruction(instr);
            curBlock.addInstruction(new ReturnInstr(returnType, instr));
        } else {
            curBlock.addInstruction(new ReturnInstr(returnType));
        }
    }

    private void init(DataType returnType, SymTab funcSymTab) {
        if (returnType == null) {
            throw new RuntimeException("会不会写函数啊，小老弟？！");
        }
        if (returnType != DataType.VOID) {
            //空串表示用于存返回值的变量
            Symbol symbol = new Symbol("", returnType, new ArrayList<>(), false, false, null);
            funcSymTab.addSym(symbol);
            curBlock.addInstruction(new AllocaInstr(curRegIndex++, symbol));
            curBlock.addInstruction(new StoreInstr(new ConstInt(0), funcSymTab.getSym("")));
        }
    }

    private void parseParams(List<Ast.FuncFParam> fParams, SymTab symTab, HashMap<Symbol, FParam> symbol2FParam) {
        if (fParams == null || symTab == null) {
            throw new NullPointerException();
        }
        String name;
        DataType dataType;
        for (Ast.FuncFParam param : fParams) {
            name = param.getName();
            switch (param.getType().getType()) {
                case INT:   dataType = DataType.INT;   break;
                case FLOAT: dataType = DataType.FLOAT; break;
                default: throw new RuntimeException("出现了未曾设想的类型");
            }
            List<Integer> limList = new ArrayList<>();
            Value initVal = null;
            if (param.isArray()) {
                limList.add(-1);    // 第一维长度不定，暂且记为 -1
                for (Ast.Exp exp : param.getArrayItemList()) {
                    if (exp.checkConstType(symTab) != DataType.INT) {
                        throw new RuntimeException("数组长度必须能计算到确定的整数");
                    }
                    limList.add(exp.getConstInt(symTab));
                }
                initVal = new ArrayInitVal(dataType, limList);
            }
            Symbol symbol = new Symbol(name, dataType, limList, false, false, initVal);
            symTab.addSym(symbol);
            FParam fParam = new FParam(curRegIndex++, dataType, symbol.getDim());
            symbol2FParam.put(symbol, fParam);
            fParamValueList.add(fParam);
        }
    }

    private void storeParams(HashMap<Symbol, FParam> symbol2FParam) {
        for (Symbol symbol : symbol2FParam.keySet()) {
            curBlock.addInstruction(new AllocaInstr(curRegIndex++, symbol));
            curBlock.addInstruction(new StoreInstr(symbol2FParam.get(symbol), symbol));
        }
    }

    public List<Value> getFParamSymbolList() {
        return this.fParamValueList;
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
        if (item instanceof Ast.Continue) {
            dealContinue();
        } else if (item instanceof Ast.Break) {
            dealBreak();
        } else if (item instanceof Ast.WhileStmt) {
            dealWhile((Ast.WhileStmt)item, returnType, symTab);
        } else if (item instanceof Ast.IfStmt) {
            dealIf((Ast.IfStmt) item, returnType, symTab);
        } else if (item instanceof Ast.Return) {
            dealReturn((Ast.Return) item, returnType, symTab);
        } else if (item instanceof Ast.Block) {
            parseCodeBlock((Ast.Block) item, returnType, new SymTab(symTab));
        } else if (item instanceof Ast.Assign) {
            dealAssign((Ast.Assign) item, symTab);
        } else if (item instanceof Ast.ExpStmt) {
            calculateExpr(((Ast.ExpStmt) item).getExp(), symTab, true);
        } else {
            throw new RuntimeException("出现了尚未支持的语句类型" + item.getClass());
        }
    }

    private void dealContinue() {
        if (whileBegins.empty()) {
            throw new RuntimeException("continue in wrong position");
        }
        curBlock.addInstruction(new JumpInstr(whileBegins.peek()));
        curBlock = new BasicBlock(curDepth);
        curBlock.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(curBlock);
    }

    private void dealBreak() {
        if (whileEnds.empty()) {
            throw new RuntimeException("break in wrong position");
        }
        curBlock.addInstruction(new JumpInstr(whileEnds.peek()));
        curBlock = new BasicBlock(curDepth);
        curBlock.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(curBlock);
    }


    private void dealWhile(Ast.WhileStmt item, DataType returnType, SymTab symTab) {
        BasicBlock condBlk = new BasicBlock(curDepth);
        BasicBlock bodyBlk = new BasicBlock(curDepth);
        BasicBlock endBlk = new BasicBlock(curDepth);

        curBlock.addInstruction(new JumpInstr(condBlk));//要为condBlk新建一个块吗

        condBlk.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(condBlk);
        curBlock = condBlk;
        Value cond = calculateLOr(item.cond, bodyBlk, endBlk, symTab);
        curBlock.addInstruction(new BranchInstr(cond, bodyBlk, endBlk));

        //fixme：if和while同时创建一个新end块，会导致没有语句
        bodyBlk.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(bodyBlk);
        curBlock = bodyBlk;
        whileBegins.push(condBlk);
        whileEnds.push(endBlk);
        bodyBlk.setDepth(++curDepth);
        dealStmt(item.body, returnType, new SymTab(symTab));
        bodyBlk.setDepth(--curDepth);
        whileBegins.pop();
        whileEnds.pop();

        endBlk.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(endBlk);
        curBlock.addInstruction(new JumpInstr(condBlk));
        curBlock = endBlk;
    }

    private void dealIf(Ast.IfStmt ifStmt, DataType returnType, SymTab symTab) {
        boolean hasElseBlk = ifStmt.elseStmt != null;
        BasicBlock thenBlk = new BasicBlock(curDepth);
        BasicBlock elseBlk = new BasicBlock(curDepth);
        BasicBlock endBlk = hasElseBlk ? new BasicBlock(curDepth) : elseBlk;

        Value cond = calculateLOr(ifStmt.condition, thenBlk, elseBlk, symTab);

        curBlock.addInstruction(new BranchInstr(cond, thenBlk, elseBlk));

        thenBlk.setLabelCnt(curBlkIndex++);
        basicBlocks.addToTail(thenBlk);
        curBlock = thenBlk;
        dealStmt(ifStmt.thenStmt, returnType, new SymTab(symTab));
        BasicBlock thenTmpBlk = curBlock;//curBlk可能会改变，但是为了正常插入语句，需要保存
        if (hasElseBlk) {
            elseBlk.setLabelCnt(curBlkIndex++);
            basicBlocks.addToTail(elseBlk);
            curBlock = elseBlk;
            dealStmt(ifStmt.elseStmt, returnType, new SymTab(symTab));
            endBlk.setLabelCnt(curBlkIndex++);
            curBlock.addInstruction(new JumpInstr(endBlk));
        } else {
            endBlk.setLabelCnt(curBlkIndex++);
        }

        thenTmpBlk.addInstruction(new JumpInstr(endBlk));
        basicBlocks.addToTail(endBlk);
        curBlock = endBlk;
    }

    public void dealReturn(Ast.Return item, DataType returnType, SymTab symTab) {
        if (item == null ||  returnType == null) {
            throw new NullPointerException();
        }
        Ast.Exp returnValue = (item).getReturnValue();
        if (returnValue == null) {
            curBlock.addInstruction(new JumpInstr(retBlock));
        } else {
            Value value = calculateExpr(returnValue, symTab, false);
            assert value.getDataType() != DataType.VOID && returnType != DataType.VOID;
            if (returnType == DataType.INT && value.getDataType() == DataType.FLOAT) {
                value = new Fp2Si(curRegIndex++, value);
                curBlock.addInstruction((Instruction) value);
            } else if (returnType == DataType.FLOAT && value.getDataType() == DataType.INT) {
                value = new Si2Fp(curRegIndex++, value);
                curBlock.addInstruction((Instruction) value);
            }
            curBlock.addInstruction(new StoreInstr(value, symTab.getSym("")));
            curBlock.addInstruction(new JumpInstr(retBlock));
        }
    }
    
    private void dealAssign(Ast.Assign item, SymTab symTab) {
        if (item == null || curBlock == null || symTab == null) {
            throw new NullPointerException();
        }
        Ast.LVal lVal = item.getLVal();
        Symbol left = symTab.getSym(lVal.getName());
        Value right = calculateExpr(item.getExp(), symTab, false);
        if (left.getType() == DataType.FLOAT && right.getDataType() == DataType.INT) {
            right = new Si2Fp(curRegIndex++, right);
            curBlock.addInstruction((Instruction) right);
        }
        if (left.getType() == DataType.INT && right.getDataType() == DataType.FLOAT) {
            right = new Fp2Si(curRegIndex++, right);
            curBlock.addInstruction((Instruction) right);
        }
        if (left.isArray()) {
            List<Value> indexList = getIndexList(lVal, symTab);
            Instruction ptr = getPtr(left, indexList);
            curBlock.addInstruction(ptr);
            curBlock.addInstruction(new StoreInstr(right, left, ptr));
        } else {
            curBlock.addInstruction(new StoreInstr(right, left));
        }
        
    }
    
    private void dealDecl(SymTab symTab, Ast.Decl item) {
        if (curBlock == null || symTab == null || item == null) {
            throw new NullPointerException();
        }
        List<Symbol> newSymList = symTab.parseNewSymbols(item);
        for (Symbol symbol : newSymList) {
            curBlock.addInstruction(new AllocaInstr(curRegIndex++, symbol));
            Value initVal = symbol.getInitVal();
            if (initVal != null) {
                if (initVal instanceof ConstValue) {
                    curBlock.addInstruction(new StoreInstr(initVal, symbol));
                } else if (initVal instanceof InitExpr) {
                    Value init = calculateExpr(((InitExpr) initVal).getExp(), symTab, false);
                    curBlock.addInstruction(new StoreInstr(init, symbol));
                } else if (initVal instanceof ArrayInitVal) {
                    ArrayList<Value> baseIndexList = new ArrayList<>();
                    for (int i = 0; i < ((ArrayInitVal) initVal).getDim(); i++) {
                        baseIndexList.add(new ConstInt(0));
                    }
                    GEPInstr toBase = new GEPInstr(curRegIndex++, baseIndexList, symbol);
                    curBlock.addInstruction(toBase);
                    Bitcast toI8 = new Bitcast(curRegIndex++, toBase);
                    curBlock.addInstruction(toI8);
                    ArrayList<Value> rParams = new ArrayList<>();
                    rParams.add(toI8);
                    rParams.add(new ConstInt(0));
                    rParams.add(new ConstInt(((ArrayInitVal) initVal).getSize()));
                    LibFunc libFunc = Lib.getInstance().getLibFunc("memset");
                    CallInstr memset = libFunc.makeCall(curRegIndex++, rParams);
                    curBlock.addInstruction(memset);
                    
                    List<List<Integer>> toInit = new ArrayList<>();
                    ((ArrayInitVal) initVal).getNonZeroIndex(toInit, new ArrayList<>());
                    for (List<Integer> list : toInit) {
                        Value valToInit = ((ArrayInitVal) initVal).getValueWithIndex(list);
                        ArrayList<Value> indexList = new ArrayList<>();
                        for (Integer index : list) {
                            indexList.add(new ConstInt(index));
                        }
                        if (valToInit instanceof ConstValue) {
                            Instruction ptr = new GEPInstr(curRegIndex++, indexList, symbol);
                            curBlock.addInstruction(ptr);
                            curBlock.addInstruction(new StoreInstr(valToInit, symbol, ptr));
                        } else if (valToInit instanceof InitExpr) {
                            Instruction ptr = new GEPInstr(curRegIndex++, indexList, symbol);
                            curBlock.addInstruction(ptr);
                            Value init = calculateExpr(((InitExpr) valToInit).getExp(), symTab, false);
                            curBlock.addInstruction(new StoreInstr(init, symbol, ptr));
                        } else {
                            throw new RuntimeException("最后一层了只能是常数或者表达式了吧");
                        }
                    }
                } else {
                    throw new RuntimeException("这什么玩意？");
                }
                
            }
            if (!symbol.isConstant()) {
                symTab.addSym(symbol);
            }
        }
    }

    private Value transform2i1(Value value) {
        DataType type = value.getDataType();

        if (type == DataType.BOOL) {
            return value;
        } else if (type == DataType.INT) {
            Instruction instr = new ICmpInstr(curRegIndex++, CmpCond.NE, value, new ConstInt(0));
            curBlock.addInstruction(instr);
            return instr;
        } else if (type == DataType.FLOAT) {
            Instruction instr = new FCmpInstr(curRegIndex++, CmpCond.NE, value, new ConstFloat(0));
            curBlock.addInstruction(instr);
            return instr;
        } else {
            throw new RuntimeException("wrong in input class");
        }
    }

    //if ( A || (B && C) || D)
    private Value calculateLOr(Ast.Exp exp, BasicBlock trueBlk, BasicBlock falseBlk, SymTab symTab) {
        if (!(exp instanceof Ast.BinaryExp)) {
            return transform2i1(calculateExpr(exp, symTab, false));
        }
        Ast.BinaryExp bin = (Ast.BinaryExp) exp;
        BasicBlock nextBlk = falseBlk;
        Value condValue = transform2i1(calculateLAnd(bin.getFirstExp(), nextBlk, symTab));


        for (int i = 0; i < bin.getOps().size(); i++) {
            nextBlk = new BasicBlock(curDepth);
            Token op = bin.getOps().get(i);
            Ast.Exp nextExp = bin.getRestExps().get(i);
            assert op.getType() == TokenType.LOR;
            curBlock.addInstruction(new BranchInstr(condValue, trueBlk, nextBlk));
            curBlock = nextBlk;
            curBlock.setLabelCnt(curBlkIndex++);
            basicBlocks.addToTail(curBlock);
            condValue = transform2i1(calculateLAnd(nextExp, falseBlk, symTab));
        }

        return condValue;
    }

    private Value calculateLAnd(Ast.Exp exp, BasicBlock falseBlk, SymTab symTab) {
        if (!(exp instanceof Ast.BinaryExp)) {
            return transform2i1(calculateExpr(exp, symTab, false));
        }
        Ast.BinaryExp bin = (Ast.BinaryExp) exp;
        BasicBlock nextBlk;
        Value condValue = transform2i1(calculateExpr(bin.getFirstExp(), symTab, false));

        for (int i = 0; i < bin.getOps().size(); i++) {
            nextBlk = new BasicBlock(curDepth);
            Token op = bin.getOps().get(i);
            Ast.Exp nextExp = bin.getRestExps().get(i);
            assert op.getType() == TokenType.LAND;
            curBlock.addInstruction(new BranchInstr(condValue, nextBlk, falseBlk));
            curBlock = nextBlk;
            curBlock.setLabelCnt(curBlkIndex++);
            basicBlocks.addToTail(curBlock);
            condValue = transform2i1(calculateExpr(nextExp, symTab, false));
        }
        return condValue;
    }
    private Value calculateExpr(Ast.Exp exp, SymTab symTab, boolean canBeEmpty) {
        if (exp == null) {
            if (canBeEmpty) {
                return null;
            }
            throw new NullPointerException();
        }
        switch (exp.checkConstType(symTab)) {
            case INT:   return new ConstInt(exp.getConstInt(symTab));
            case FLOAT: return new ConstFloat(exp.getConstFloat(symTab));
        }
        if (exp instanceof Ast.BinaryExp) {
            Value firstValue = calculateExpr(((Ast.BinaryExp) exp).getFirstExp(), symTab, canBeEmpty);
            List<Ast.Exp> rest = ((Ast.BinaryExp) exp).getRestExps();
            for (int i = 0; i < rest.size(); i++) {
                Ast.Exp expI = rest.get(i);
                Token op = ((Ast.BinaryExp) exp).getOps().get(i);
                Value valueI = calculateExpr(expI, symTab, canBeEmpty);
                
                DataType type1 = firstValue.getDataType();
                DataType type2 = valueI.getDataType();
                
                if (type1 == DataType.VOID || type2 == DataType.VOID) {
                    throw new RuntimeException("二元表达式不能处理 void");
                }
                
                // i1 -> i32
                if (type1 == DataType.BOOL) {
                    Instruction instruction = new Zext(curRegIndex++, firstValue);
                    curBlock.addInstruction(instruction);
                    firstValue = instruction;
                    type1 = DataType.INT;
                }
                if (type2 == DataType.BOOL) {
                    Instruction instruction = new Zext(curRegIndex++, valueI);
                    curBlock.addInstruction(instruction);
                    valueI = instruction;
                    type2 = DataType.INT;
                }
                
                // 统一数据类型
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
                    case EQ:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.EQ, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.EQ, firstValue, valueI);
                        }
                        break;
                    case NE:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.NE, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.NE, firstValue, valueI);
                        }
                        break;
                    case LT:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.LT, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.LT, firstValue, valueI);
                        }
                        break;
                    case LE:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.LE, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.LE, firstValue, valueI);
                        }
                        break;
                    case GT:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.GT, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.GT, firstValue, valueI);
                        }
                        break;
                    case GE:
                        if(dataType == DataType.FLOAT) {
                            instruction = new FCmpInstr(curRegIndex++, CmpCond.GE, firstValue, valueI);
                        } else {
                            instruction = new ICmpInstr(curRegIndex++, CmpCond.GE, firstValue, valueI);
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
                res = calculateExpr((Ast.Exp) primary, symTab, canBeEmpty);
            } else if (primary instanceof Ast.Call) {
                res = dealCall((Ast.Call) primary, symTab);
            } else if (primary instanceof Ast.LVal) {
                Symbol symbol = symTab.getSym(((Ast.LVal) primary).getName());
                if (symbol.isArray()) {
                    List<Value> indexList = getIndexList((Ast.LVal) primary, symTab);
                    GEPInstr ptr = getPtr(symbol, indexList);
                    curBlock.addInstruction(ptr);
                    if (ptr.getPointerLevel() == 1) {
                        Instruction load = new LoadInstr(curRegIndex++, symbol, ptr);
                        curBlock.addInstruction(load);
                        res = load;
                    } else {
                        Instruction newPtr = new GEPInstr(curRegIndex++, ptr);
                        curBlock.addInstruction(newPtr);
                        res = newPtr;
                    }
                } else {
                    if (symbol.isConstant() || symTab.isGlobal() && symbol.isGlobal()) {
                        res = symbol.getInitVal();
                    } else {
                        Instruction load = new LoadInstr(curRegIndex++, symbol);
                        curBlock.addInstruction(load);
                        res = load;
                    }
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
                        res = new SubInstr(curRegIndex++, new ConstInt(0), res);
                        break;
                    case FLOAT:
                        res = new FNegInstr(curRegIndex++, res);
                        break;
                    default:
                        throw new RuntimeException("带符号的指令不能，至少不应该连返回值都没有吧");
                }
                curBlock.addInstruction((Instruction) res);
            }
            if (((Ast.UnaryExp) exp).checkNot()) {
                DataType dataType = res.getDataType();
                if (dataType == DataType.INT) {
                    res = new ICmpInstr(curRegIndex++, CmpCond.EQ, new ConstInt(0), res);
                } else if (dataType == DataType.FLOAT) {
                    res = new FCmpInstr(curRegIndex++, CmpCond.EQ, new ConstFloat(0), res);
                } else {
                    throw new RuntimeException("!后面返回的应该只能是整数或者浮点数");
                }
                curBlock.addInstruction((Instruction) res);
            }
            return res;
        } else {
            throw new RuntimeException("奇怪的表达式");
        }
    }
    
    private GEPInstr getPtr(Symbol symbol, List<Value> indexList){
        GEPInstr ptr;
        if (symbol.isArrayFParam()) {
            LoadInstr load = new LoadInstr(curRegIndex++, symbol);
            curBlock.addInstruction(load);
            ptr = new GEPInstr(curRegIndex++, load, indexList);
        } else {
            ptr = new GEPInstr(curRegIndex++, indexList, symbol);
        }
        return ptr;
    }

    private List<Value> getIndexList(Ast.LVal lVal, SymTab symTab) {
        if (lVal == null) {
            throw new NullPointerException();
        }
        ArrayList<Value> indexList = new ArrayList<>();
        for (Ast.Exp innerexp : lVal.getIndexList()) {
            Value innerRes = calculateExpr(innerexp, symTab, false);
            if (innerRes.getDataType() != DataType.INT) {
                throw new RuntimeException("数组下标不是整数？");
            }
            if (innerRes instanceof Instruction) {
                innerRes = new Sext(curRegIndex++, innerRes);
                curBlock.addInstruction((Instruction) innerRes);
            }
            indexList.add(innerRes);
        }
        return indexList;
    }
    
    private Value dealCall(Ast.Call call, SymTab symTab) {
        if (call == null) {
            throw new NullPointerException();
        }
        ArrayList<Value> rParams = new ArrayList<>();
        for (Ast.Exp exp : call.getParams()) {
            rParams.add(calculateExpr(exp, symTab, false));
        }
        String name = call.getName();
        if (name.equals("starttime") || name.equals("stoptime")) {
            if (!rParams.isEmpty()) {
                throw new RuntimeException("计时函数不应该显式传参");
            }
            rParams.add(new ConstInt(0));   //这里应该传行号，但是 AST 暂时不支持行号，喵喵队也没传，索性不传了
        }
        CallInstr callInstr;
        LibFunc libFunc = Lib.getInstance().getLibFunc(name);
        // todo: 数组（指针）类型可能会转换吗？反正现在是不能处理的。。。
        // 现在有 bitcast 指令了，但是暂时没有处理一般化的转换，只做了 memset
        if (libFunc != null) {
            List<Ast.FuncFParam> libFParams = libFunc.getFParams();
            funcParamConv(libFParams, rParams);
            callInstr = libFunc.makeCall(curRegIndex++, rParams);
        } else {
            Function myFunc = Function.getFunction(name);
            if (myFunc == null) {
                throw new RuntimeException("不是哥们，你这是什么函数啊？" + name);
            }
            List<Ast.FuncFParam> fParams = myFunc.getFParams();
            funcParamConv(fParams, rParams);
            callInstr = Function.makeCall(curRegIndex++, name, rParams);
        }
        if (callInstr.getDataType() == DataType.VOID) {
            curRegIndex--;
        }
        curBlock.addInstruction(callInstr);
        return callInstr;
    }

    private void funcParamConv(List<Ast.FuncFParam> fParams, List<Value> rParams) {
        for (int i = 0; i < fParams.size(); i++) {
            Value curRParam = rParams.get(i);
            DataType rType = curRParam.getDataType();
            TokenType fType = fParams.get(i).getType().getType();
            if ((rType != DataType.INT && rType != DataType.FLOAT)
                    || (fType != TokenType.INT && fType != TokenType.FLOAT)) {
                throw new RuntimeException("形参实参出现了意料之外的东西");
            }
            if (rType == DataType.INT && fType == TokenType.FLOAT) {
                ConversionOperation conv = new Si2Fp(curRegIndex++, curRParam);
                curBlock.addInstruction(conv);
                rParams.set(i, conv);
            }
            if (rType == DataType.FLOAT && fType == TokenType.INT) {
                ConversionOperation conv = new Fp2Si(curRegIndex++, curRParam);
                curBlock.addInstruction(conv);
                rParams.set(i, conv);
            }
        }
    }
    
    public void printIR(Writer writer) throws IOException {
        if (writer == null) {
            throw new NullPointerException();
        }
        for (CustomList.Node basicBlockNode : basicBlocks) {
            BasicBlock block = (BasicBlock) basicBlockNode;
            writer.append(block.value2string()).append(":\n");
            block.printIR(writer);
        }
        DEBUG.dbgPrint1("\n");
        for (CustomList.Node basicBlockNode : basicBlocks) {
            BasicBlock block = (BasicBlock) basicBlockNode;
            DEBUG.dbgPrint1(block.value2string());
            block.printDBG();
        }
    }

    public CustomList getBasicBlocks() {
        return basicBlocks;
    }
}
