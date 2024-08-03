package frontend.ir.structure;

import Utils.CustomList;
import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.otherop.PhiInstr;
import frontend.ir.instr.terminator.ReturnInstr;
import frontend.ir.symbols.SymTab;
import frontend.lexer.TokenType;
import frontend.syntax.Ast;
import midend.loop.Loop;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Function extends Value implements FuncDef {
    private static final HashMap<String, Function> FUNCTION_MAP = new HashMap<>();
    private static final int whatIsLong = 100;  // 一个函数多少指令算是“多”，不能内联
    private final String name;
    private final DataType returnType;
    private final Procedure procedure;
    private final List<Ast.FuncFParam> astFParams;
    private final List<FParam> fParams;
    private final HashSet<Function> myImmediateCallee = new HashSet<>(); // 被 this 直接调用的自定义函数列表
    private final HashSet<Function> allCallee = new HashSet<>(); // 本函数执行过程中会调用（包括间接调用）的所有函数，可能包括自己
    private ArrayList<Loop> allLoop = new ArrayList<>();
    private HashMap<BasicBlock, Loop> header2loop = new HashMap<>();
    private ArrayList<Loop> outerLoop = new ArrayList<>();
    private int calledCnt = 0;
    private boolean isTailRecursive = false;
    private final boolean main;
    private boolean neverUsed = false;
    
    public Function(Ast.FuncDef funcDef, SymTab globalSymTab) {
        if (funcDef == null) {
            throw new NullPointerException();
        }
        name = funcDef.getIdent().getContent();
        main = name.equals("main");
        SymTab symTab = new SymTab(globalSymTab);
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
        astFParams = funcDef.getFParams();
        fParams = new ArrayList<>();
        FUNCTION_MAP.put(name, this);
        procedure = new Procedure(returnType, astFParams, funcDef.getBody(), symTab, myImmediateCallee, this, fParams);
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
        int len = fParams.size();
        for (int i = 0; i < len; i++) {
            FParam fParam = fParams.get(i);
            writer.append(fParam.type2string());
            writer.append(" ").append(fParam.value2string());
            if (i < len - 1) {
                writer.append(", ");
            }
        }
    }
    
    public List<FParam> getFParamValueList() {
        return this.procedure.getFParamValueList();
    }

    public CustomList getBasicBlocks() {
        return procedure.getBasicBlocks();
    }

    public CallInstr makeCall(int result, List<Value> rParams) {
        if (rParams == null) {
            throw new NullPointerException();
        }
        if (!this.checkParams(rParams)) {
            throw new RuntimeException("形参实参不匹配");
        }
        DataType type = this.getDataType();
        if (type == DataType.VOID) {
            return new CallInstr(null, type, this, rParams);
        } else {
            return new CallInstr(result, type, this, rParams);
        }
    }

    private boolean checkParams(List<Value> rParams) {
        if (rParams == null) {
            throw new NullPointerException();
        }
        if (rParams.size() != astFParams.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < astFParams.size(); i++) {
            if (rParams.get(i).getDataType() == DataType.INT &&
                    astFParams.get(i).getType().getType() != TokenType.INT) {
                throw new RuntimeException();
            }
            if (rParams.get(i).getDataType() == DataType.FLOAT &&
                    astFParams.get(i).getType().getType() != TokenType.FLOAT) {
                throw new RuntimeException();
            }
        }
        return true;
    }
    
    public List<Ast.FuncFParam> getFParams() {
        return astFParams;
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
        List<FParam> fParamValueList = this.procedure.getFParamValueList();
        for (int i = 0; i < fParamValueList.size(); i++) {
            old2new.put(fParamValueList.get(i), rParams.get(i));
        }
        
        BasicBlock curBB = (BasicBlock) this.procedure.getBasicBlocks().getHead();
        while (curBB != null) {
            BasicBlock newBB = new BasicBlock(curDepth + curBB.getLoopDepth(), caller.getAndAddBlkIndex());
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
                if (newIns instanceof PhiInstr) {
                    ((PhiInstr) newIns).renewBlocks(old2new);
                }
                
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
    
    /**
     * 用于将所有的 alloca 都集中到函数最开始以避免反复申请内存；
     */
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
    
    public int getCalledCnt() {
        return calledCnt;
    }
    
    public void updateUse() {
        if (this.main) {
            this.neverUsed = false;
            return;
        }
        this.neverUsed = this.calledCnt <= 0;
    }
    
    public boolean noUse() {
        return this.neverUsed;
    }

    public Procedure getProcedure() {
        return procedure;
    }
    
    public boolean checkInsTooMany() {
        int cnt = 0;
        BasicBlock basicBlock = (BasicBlock) this.procedure.getBasicBlocks().getHead();
        while (basicBlock != null) {
            Instruction instruction = (Instruction) basicBlock.getInstructions().getHead();
            while (instruction != null) {
                if (++cnt > whatIsLong) {
                    return true;
                }
                instruction = (Instruction) instruction.getNext();
            }
            basicBlock = (BasicBlock) basicBlock.getNext();
        }
        return false;
    }

    public void setAllLoop(ArrayList<Loop> allLoop) {
        this.allLoop = allLoop;
    }

    public void setHeader2loop(HashMap<BasicBlock, Loop> header2loop) {
        this.header2loop = header2loop;
    }

    public void setOuterLoop(ArrayList<Loop> outerLoop) {
        this.outerLoop = outerLoop;
    }

    public ArrayList<Loop> getAllLoop() {
        return allLoop;
    }

    public ArrayList<Loop> getOuterLoop() {
        return outerLoop;
    }

    public HashMap<BasicBlock, Loop> getHeader2loop() {
        return header2loop;
    }

    public int getPhiIndex() {
        return procedure.getPhiIndex();
    }

    public void setCurPhiIndex(int curPhiIndex) {
        procedure.setCurPhiIndex(curPhiIndex);
    }

    public HashSet<CallInstr> getSelfCallingInstrSet() {
        return this.procedure.getSelfCallingInstrSet();
    }

    public boolean isMain() {
        return main;
    }

    public void setTailRecursive(boolean tailRecursive) {
        isTailRecursive = tailRecursive;
    }

    public boolean isTailRecursive() {
        return isTailRecursive;
    }

    /**
     * 目前检查函数无副作用的标准就是没有 I/O 且没有修改内存
     * 自定义函数肯定没有 I/O
     */
    public boolean checkNoSideEffect() {
        BasicBlock basicBlock = (BasicBlock) this.procedure.getBasicBlocks().getHead();
        while (basicBlock != null) {
            Instruction ins = (Instruction) basicBlock.getInstructions().getHead();
            while (ins != null) {
                if (ins instanceof StoreInstr) {
                    return false;
                }
                ins = (Instruction) ins.getNext();
            }
            basicBlock = (BasicBlock) basicBlock.getNext();
        }

        for (Function function : myImmediateCallee) {
            if (function == this) {
                return true;
            } else {
                return function.checkNoSideEffect();
            }
        }

        return true;
    }
}
