package backend;

import backend.asmInstr.AsmMove;
import backend.itemStructure.*;
import backend.regs.AsmVirReg;
import frontend.ir.BasicBlock;
import frontend.ir.Program;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.instr.Instruction;
import frontend.ir.symbols.Symbol;
import frontend.ir.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.regs.RegGeter.ZERO;

public class IrParser {
    private Program program;
    private AsmModule asmModule;
    private HashMap<Symbol, AsmGlobalVar> gvMap;
    private HashMap<Function, AsmFunction> funcMap;
    private HashMap<BasicBlock, AsmBlock> blockMap;
    private HashMap<Value, AsmOperand> operandMap;


    public IrParser(Program program) {
        this.program = program;
        this.asmModule = new AsmModule();
        this.gvMap = new HashMap<>();
        setLibFunctions();
    }

    public AsmModule parse(Program program) {
        parseGlobalVars();
        parseFunctions();
    }

    private void parseGlobalVars() {
        HashSet<Symbol> globalVars = program.getGlobalVars();
        for (Symbol globalVar : globalVars) {
            AsmGlobalVar asmGlobalVar = parseGlobalVar(globalVar);
            asmModule.addGlobalVar(asmGlobalVar);
            gvMap.put(globalVar, asmGlobalVar);
        }
    }

    private AsmGlobalVar parseGlobalVar(Symbol globalVar) {
        //TODO: 指针类型？Type的isPointerType？
        AsmType type = globalVar.getAsmType();
        ArrayList<Integer> items = new ArrayList<>();
        if (type == AsmType.INT) {
            int intVal = globalVar.getValue(AsmType.INT).intValue();
            items.add(intVal);
        } else if (type == AsmType.FLOAT) {
            float floatVal = globalVar.getValue(AsmType.FLOAT).floatValue();
            items.add(Float.floatToIntBits(floatVal));
        } else if (type == AsmType.ARRAY) {
            //TODO:暂时还未完成数组赋值
        }
        if (items.isEmpty()) {
            //TODO:Array有待实现
            int offsetSize = (type == AsmType.ARRAY) ? 4 * globalVar.getArraySize() : 4;
            return new AsmGlobalVar(globalVar.getName(), offsetSize);
        } else {
            return new AsmGlobalVar(globalVar.getName(), items);
        }
    }

    private void parseFunctions() {
        createMaps();
        for (Function f : program.getFunctions().values()) {
            if (!f.isLib()) {
                parseFunction(f);
            }
        }
    }

    private void parseFunction(Function f) {
        AsmFunction asmFunction = funcMap.get(f);
        int pushArgSize = 0;
        for (BasicBlock bb : f.getBasicBlocks()) {
            for (Instruction instr : bb.getInstructions()) {
                if (instr.isCall()) {
                    //TODO：处理栈预留空间，有尾递归处理
                }
            }
        }
        //+8是ra的大小
        asmFunction.addStackSize(pushArgSize + 8);

        for (BasicBlock bb : f.getBasicBlocks()) {
            parseBlock(bb, f);
        }

        //TODO:需要函数退出块。为什么需要？好像是寄存器分配部分要。
        asmFunction.setExit(blockMap.get(f.getExit()));
        asmFunction.setIsTail(f.isTail());

        //TODO:fargnum与iargnum有什么用？不知道，先省略了。

    }

    private void createMaps() {
        for (Function f : program.getFunctions().values()) {
            AsmFunction asmFunction = new AsmFunction(f.getName(), f.isLib());
            asmModule.addFunction(asmFunction);
            funcMap.put(f, asmFunction);
            //TODO:双向链表为什么需要？有待商榷
            int blockIndex = 0;
            for (BasicBlock bb : f.getBasicBlocks()) {
                AsmBlock asmBlock = new AsmBlock(blockIndex++);
                asmFunction.addBlock(asmBlock);
                blockMap.put(bb, asmBlock);
            }
            //TODO:基于双向链表
        }
    }

    private AsmOperand parseOperand(Value irValue, int maxImm, Function irFunction, BasicBlock bb) {
        if (operandMap.containsKey(irValue)) {
            AsmOperand asmOperand = operandMap.get(irValue);
            if (((asmOperand instanceof AsmImm32) && maxImm < 32) ||
                    ((asmOperand instanceof AsmImm12) && maxImm < 12)) {
                if ((asmOperand instanceof AsmImm32) && (((AsmImm32) asmOperand).getImm() == 0)) {
                    return ZERO;
                }
                if ((asmOperand instanceof AsmImm12) && (((AsmImm12) asmOperand).getImm() == 0)) {
                    return ZERO;
                }
                //TODO:生成临时寄存器
                AsmOperand tmpReg = genTmpReg(irFunction);
                AsmMove asmMove = new AsmMove(tmpReg, asmOperand);
                blockMap.get(bb).addInst(asmMove);
                return tmpReg;
            }
            return asmOperand;
        }

        //TODO:关于move指令的到时候再写吧
        if(irValue instanceof ConstInt){
            return parseConstIntOperand(((ConstInt) irValue).getValue(),maxImm,irFunction,bb);
        }




    }

    private AsmOperand parseConstIntOperand(int value, int maxImm, Function irFunction, BasicBlock bb) {
        AsmImm32 asmImm32 = new AsmImm32(value);
        AsmImm12 asmImm12 = new AsmImm12(value);
        if(maxImm==32){
            return asmImm32;
        }
        if(maxImm==12&&(value>=-2048&&value<=2047)){
            return asmImm12;
        }
        AsmBlock asmBlock = blockMap.get(bb);
        AsmVirReg tmpReg = genTmpReg(irFunction);
        AsmMove asmMove = new AsmMove(tmpReg,asmImm32);
        asmBlock.addInstrTail(asmMove);
        return tmpReg;
    }

    private AsmVirReg genTmpReg(Function irFunction) {
        AsmFunction asmFunction = funcMap.get(irFunction);
        AsmVirReg tmpReg = new AsmVirReg();
        asmFunction.addUsedVirReg(tmpReg);
        return tmpReg;
    }


    private void setLibFunctions() {
        //TODO:设置库函数
    }


}
