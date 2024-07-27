package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.symbols.SymTab;
import frontend.ir.symbols.Symbol;
import frontend.lexer.TokenType;
import frontend.syntax.Ast;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Function extends Value implements FuncDef {
    private static final HashMap<String, Function> FUNCTION_MAP = new HashMap<>();
    private final String name;
    private final DataType returnType;
    private final Procedure procedure;
    private final SymTab symTab;
    private final List<Ast.FuncFParam> fParams;
    private final HashSet<Function> myImmediateCallee = new HashSet<>(); // 被 this 直接调用的自定义函数列表
    private final HashSet<Function> allCallee = new HashSet<>(); // 本函数执行过程中会调用（包括间接调用）的所有函数，可能包括自己
    private int calledCnt = 0;

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
        procedure = new Procedure(returnType, fParams, funcDef.getBody(), symTab, myImmediateCallee);
        initAllCallee();
    }
    
    public static Function getFunction(String name) {
        return FUNCTION_MAP.get(name);
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
            DataType type;
            switch (param.getType().getType()) {
                case INT:   type = DataType.INT;   break;
                case FLOAT: type = DataType.FLOAT; break;
                default: throw new RuntimeException("出现了奇怪的形参类型");
            }
            
            if (param.isArray()) {
                ArrayList<Integer> limList = new ArrayList<>();
                limList.add(-1);
                for (Ast.Exp exp : param.getArrayItemList()) {
                    if (exp.checkConstType(symTab) != DataType.INT) {
                        throw new RuntimeException("数组维度长度必须能求值到整数");
                    }
                    limList.add(exp.getConstInt(symTab));
                }
                StringBuilder stringBuilder = new StringBuilder();
                int lim = limList.size();
                for (int j = 1; j < lim; j++) {
                    stringBuilder.append("[").append(limList.get(j)).append(" x ");
                }
                stringBuilder.append(type);
                for (int j = 1; j < lim; j++) {
                    stringBuilder.append("]");
                }
                writer.append(stringBuilder.append("*").toString());
            } else {
                writer.append(type.toString());
            }
            writer.append(" %reg_").append(Integer.toString(i));
            if (i < fParams.size() - 1) {
                writer.append(", ");
            }
        }
    }
    
    public List<Value> getFParamValueList() {
        return this.procedure.getFParamValueList();
    }

    public CustomList getBasicBlocks() {
        return procedure.getBasicBlocks();
    }

    public List<Symbol> getArgs(){
        return symTab.getSymbolList();
    }

    public static CallInstr makeCall(int result, String name, List<Value> rParams) {
        if (rParams == null || name == null) {
            throw new NullPointerException();
        }
        Function function = FUNCTION_MAP.get(name);
        if (function == null) {
            throw new RuntimeException("兄弟，这是最后的防线了，真没有别的函数了");
        }
        if (!function.checkParams(rParams)) {
            throw new RuntimeException("形参实参不匹配");
        }
        DataType type = function.getDataType();
        if (type == DataType.VOID) {
            return new CallInstr(null, type, function, rParams);
        } else {
            return new CallInstr(result, type, function, rParams);
        }
    }

    private boolean checkParams(List<Value> rParams) {
        if (rParams == null) {
            throw new NullPointerException();
        }
        if (rParams.size() != fParams.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < fParams.size(); i++) {
            if (rParams.get(i).getDataType() == DataType.INT &&
                    fParams.get(i).getType().getType() != TokenType.INT) {
                throw new RuntimeException();
            }
            if (rParams.get(i).getDataType() == DataType.FLOAT &&
                    fParams.get(i).getType().getType() != TokenType.FLOAT) {
                throw new RuntimeException();
            }
        }
        return true;
    }
    
    public List<Ast.FuncFParam> getFParams() {
        return fParams;
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
    
    @Override
    public String toString() {
        return this.name;
    }

    public LinkedList<Instruction> getAllInstr() {
        LinkedList<Instruction> list = new LinkedList<>();
        BasicBlock block = (BasicBlock) this.getBasicBlocks().getHead();
        while (block != null) {
            Instruction instr = (Instruction) block.getInstructions().getHead();
            while (instr != null) {
                list.add(instr);
                instr = (Instruction) instr.getNext();
            }
            block = (BasicBlock) block.getNext();
        }
        return list;
    }
    
    public int getAndAddRegIndex() {
        return this.procedure.getAndAddRegIndex();
    }
    
    public int getAndAddBlkIndex() {
        return this.procedure.getAndAddBlkIndex();
    }
    
    public boolean isRecursive() {
        return this.allCallee.contains(this);
    }
    
    private void initAllCallee() {
        for (Function callee : myImmediateCallee) {
            if (callee != this) {
                this.allCallee.addAll(callee.allCallee);
            }
            this.allCallee.add(callee);
        }
    }
    
    public ArrayList<BasicBlock> func2blocks(int curDepth, List<Value> rParams, Function caller) {
        ArrayList<BasicBlock> bbs = new ArrayList<>();
        HashMap<Value, Value> old2new = new HashMap<>();
        
        // 准备替换参数
        List<Value> fParamValueList = this.procedure.getFParamValueList();
        for (int i = 0; i < fParamValueList.size(); i++) {
            old2new.put(fParamValueList.get(i), rParams.get(i));
        }
        
        BasicBlock curBB = (BasicBlock) this.procedure.getBasicBlocks().getHead();
        while (curBB != null) {
            BasicBlock newBB = new BasicBlock(curDepth + curBB.getDepth(), caller.getAndAddBlkIndex());
            old2new.put(curBB, newBB);
            bbs.add(newBB);
            
            Instruction curIns = (Instruction) curBB.getInstructions().getHead();
            while (curIns != null) {
                Instruction newIns = curIns.cloneShell(caller);
                newBB.addInstruction(newIns);
                old2new.put(curIns, newIns);
                curIns = (Instruction) curIns.getNext();
            }
            
            curBB = (BasicBlock) curBB.getNext();
        }
        
        for (BasicBlock newBB : bbs) {
            Instruction newIns = (Instruction) newBB.getInstructions().getHead();
            while (newIns != null) {
                ArrayList<Value> usedValues = new ArrayList<>(newIns.getUseValueList());
                for (Value toReplace : usedValues) {
                    if (!old2new.containsKey(toReplace)) {
                        if (newIns instanceof ReturnInstr && toReplace == newBB) {
                            continue;
                        }
                        if (!(toReplace instanceof ConstValue) && !(toReplace instanceof GlobalObject)) {
                            throw new RuntimeException("使用了未曾设想的 value");
                        }
                    } else {
                        newIns.modifyUse(toReplace, old2new.get(toReplace));
                    }
                }
                newIns = (Instruction) newIns.getNext();
            }
        }
        return bbs;
    }
    
    public void allocaRearrangement() {
        this.procedure.allocaRearrangement();
    }
    
    public static void blkLabelReorder() {
        for (Function function : FUNCTION_MAP.values()) {
            int label = 0;
            BasicBlock curBB = (BasicBlock) function.procedure.getBasicBlocks().getHead();
            while (curBB != null) {
                curBB.setLabelCnt(label++);
                curBB = (BasicBlock) curBB.getNext();
            }
        }
    }
    
    public void addCall() {
        this.calledCnt++;
    }
    
    public void minusCall() {
        this.calledCnt--;
    }
    
    public boolean noUse() {
        if (this.name.equals("main")) {
            return false;
        }
        return this.calledCnt == 0;
    }
}
