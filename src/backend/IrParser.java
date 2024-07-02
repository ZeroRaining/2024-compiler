package backend;

import backend.asmInstr.asmLS.AsmMove;
import backend.asmInstr.asmBinary.AsmAdd;
import backend.asmInstr.asmLS.AsmFlw;
import backend.asmInstr.asmLS.AsmLw;
import backend.asmInstr.asmRet.AsmRet;
import backend.itemStructure.*;
import backend.regs.AsmVirReg;
import backend.regs.RegGeter;
import frontend.ir.instr.terminator.*;
import frontend.ir.instr.memop.*;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Program;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.instr.Instruction;
import frontend.ir.symbols.Symbol;
import frontend.ir.structure.Function;
import Utils.CustomList.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static backend.regs.RegGeter.ZERO;
import static frontend.ir.DataType.INT;

public class IrParser {
    private Program program;
    private AsmModule asmModule = new AsmModule();
    private HashMap<Symbol, AsmGlobalVar> gvMap = new HashMap<>();
    private HashMap<Function, AsmFunction> funcMap = new HashMap<>();
    private HashMap<BasicBlock, AsmBlock> blockMap = new HashMap<>();
    private HashMap<Value, AsmOperand> operandMap = new HashMap<>();

    public IrParser(Program program) {
        this.program = program;
    }


    public AsmModule parse(Program program) {
        parseGlobalVars();
        parseFunctions();
        return null;//待删
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
        //TODO: 依赖于指针类型的实现
        AsmType type = globalVar.getAsmType();
        ArrayList<Integer> items = new ArrayList<>();
        if (type == AsmType.INT) {
            Number initVal = globalVar.getValue();
            if (initVal != null) {
                int intVal = initVal.intValue();
                items.add(intVal);
            }
        } else if (type == AsmType.FLOAT) {
            Number initVal = globalVar.getValue();
            if (initVal != null) {
                float floatVal = initVal.floatValue();
                int intVal = Float.floatToRawIntBits(floatVal);
                items.add(intVal);
            }
        } else if (type == AsmType.ARRAY) {
            //TODO:依赖于数组类型的实现
        }
        if (items.isEmpty()) {
            //TODO:依赖于数组类型的实现
            /*int offsetSize = (type == AsmType.ARRAY) ? 4 * globalVar.getArraySize() : 4;
            return new AsmGlobalVar(globalVar.getName(), offsetSize);*/
            return null;//待删
        } else {
            return new AsmGlobalVar(globalVar.getName(), items);
        }
    }

    private void parseFunctions() {
        createMaps();
        for (Function f : program.getFunctions().values()) {
            //TODO:依赖于库函数的实现
            /*
            if (!f.isLib()) {
                parseFunction(f);
            }*/
        }
    }

    private void createMaps() {
        for (Function f : program.getFunctions().values()) {
            //TODO:依赖于库函数的实现
            /*AsmFunction asmFunction = new AsmFunction(f.getName(), f.isLib());*/
            AsmFunction asmFunction = null;//待删
            asmModule.addFunction(asmFunction);
            funcMap.put(f, asmFunction);
            int blockIndex = 0;
            for (Node bb : f.getBasicBlocks()) {
                //基于新的块编号策略
                AsmBlock asmBlock = new AsmBlock(blockIndex++);
                asmFunction.addBlock(asmBlock);
                blockMap.put((BasicBlock) bb, asmBlock);
            }
            //TODO:依赖于BasicBlock前驱后继的实现。此外，什么是loopDepth？
        }
    }

    private void parseFunction(Function f) {
        AsmFunction asmFunction = funcMap.get(f);
        int pushArgSize = 0;
        for (Node bb : f.getBasicBlocks()) {
            for (Node instr : ((BasicBlock) bb).getInstructions()) {
                //TODO:依赖于函数调用指令的实现以及尾递归的实现
                /*
                if (instr.isCall()) {
                    //处理栈预留空间，有尾递归处理
                }*/
            }
        }
        //+8是ra的大小
        asmFunction.setArgsSize(pushArgSize);
        asmFunction.setRaSize(8);

        for (Node bb : f.getBasicBlocks()) {
            parseBlock((BasicBlock) bb, f);
        }

        //TODO:需要函数退出块。为什么需要？好像是寄存器分配部分要。
        /*
        asmFunction.setExit(blockMap.get(f.getExit()));
        asmFunction.setIsTail(f.isTail());*/

        //TODO:这部分缺的东西似乎还很多，不想写了~
        HashSet<Symbol> args = f.getArgs();
        HashSet<Symbol> iargs = new HashSet<>();
        HashSet<Symbol> fargs = new HashSet<>();
        for (Symbol arg : args) {
            if (arg.getAsmType() == AsmType.INT) {
                iargs.add(arg);
            } else {
                fargs.add(arg);
            }
        }
        int iargnum = Math.min(iargs.size(), 8);
        int fargnum = Math.min(fargs.size(), 8);
        for (int i = 0; i < iargnum; i++) {
        }
    }

    private void parseBlock(BasicBlock bb, Function f) {
        for (Node instr : bb.getInstructions()) {
            parseInstr((Instruction) instr, f, bb);
        }
    }

    private void parseInstr(Instruction instr, Function f, BasicBlock bb) {
        //TODO:还有很多指令没有写qaq
        /*
        if (instr instanceof ReturnInstr)
            parseRet((ReturnInstr) instr, bb, f);
        else if (instr instanceof AllocaInstr)
            parseAlloca((AllocaInstr) instr, bb, f);
        else if (instr instanceof LoadInstr)
            parseLoad((LoadInstr) instr, bb, f);
        else if (instr instanceof StoreInstr)
            parseStore((StoreInstr) instr, bb, f);
        else if (instr instanceof CmpInstr)
            parseCmp((CmpInstr) instr, bb, f);
        else if (instr instanceof AddInstr)
            parseAdd((AddInstr) instr, bb, f);
        else if (instr instanceof FAddInstr)
            parseFAdd((FAddInstr) instr, bb, f);
        else if (instr instanceof SubInstr)
            parseSub((SubInstr) instr, bb, f);
        else if (instr instanceof FSubInstr)
            parseFSub((FSubInstr) instr, bb, f);
        else if (instr instanceof MulInstr)
            parseMul((MulInstr) instr, bb, f);
        else if (instr instanceof FMulInstr)
            parseFMul((FMulInstr) instr, bb, f);
        else if (instr instanceof SDivInstr)
            parseSDiv((SDivInstr) instr, bb, f);
        else if (instr instanceof FDivInstr)
            parseFDiv((FDivInstr) instr, bb, f);*/
    }

    private void parseRet(ReturnInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        Value retValue = instr.getReturnValue();
        if (retValue != null) {
            //返回值装a0?感觉可以优化
            //TODO:依赖于move指令的实现
            if (f.getReturnType() == INT) {
                AsmOperand asmOperand = parseOperand(retValue, 32, f, bb);
                AsmMove asmMove = new AsmMove(RegGeter.AregsInt.get(0), asmOperand);
                asmBlock.addInstrTail(asmMove);
            } else {
                AsmOperand asmOperand = parseOperand(retValue, 32, f, bb);
                AsmMove asmMove = new AsmMove(RegGeter.AregsInt.get(0), asmOperand);
                asmBlock.addInstrTail(asmMove);
            }
        }
        asmBlock.addInstrTail(new AsmRet());
    }

    private void parseAlloca(AllocaInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmFunction asmFunction = funcMap.get(f);
        /*DataType type = instr.getDataType();*/
        //动帧指针
        int offset = asmFunction.getArgsSize() + asmFunction.getAllocaSize();
        AsmOperand offOp = parseConstIntOperand(offset, 12, f, bb);
        if (true/*TODO:数组类型*/) {
            assert true;
        } else if (true/*TODO:指针类型,可能会在分配高维数组的时候出现？*/) {
            assert true;
        } else {
            asmFunction.addAllocaSize(4);
        }
        AsmOperand virS0 = parseOperand(instr, 0, f, bb);
        AsmAdd asmAdd = new AsmAdd(virS0, RegGeter.SP, offOp);
        asmBlock.addInstrTail(asmAdd);
    }

    private void parseLoad(LoadInstr instr, BasicBlock bb, Function f) {
        //%4 = load float, float* @g       la s0,g     ;  lw s1,0(s0)
        //dst                    addr
        AsmBlock asmBlock = blockMap.get(bb);
        Symbol addr = instr.getSymbol();
        if (addr.isGlobal()) {
            AsmOperand laReg = genTmpReg(f);
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            //TODO:Symbol和Value能否统一？
            /*AsmOperand src = parseOperand(addr, 0, f, bb);*/
            /*AsmLa asmLa = new AsmLa(laReg, src);*/
            AsmOperand offset = new AsmImm12(0);
            if (dst instanceof AsmVirReg) {
                AsmLw asmLw = new AsmLw(dst, laReg, offset);
                asmBlock.addInstrTail(asmLw);
            } else {
                AsmFlw asmFLw = new AsmFlw(dst, laReg, offset);
                asmBlock.addInstrTail(asmFLw);
            }
        } else if (true/*TODO:指针类型*/) {
            assert true;
        } else {
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            //TODO:同上，Symbol和Value能否统一？
            /*AsmOperand src = parseOperand(addr, 0, f, bb);*/
            AsmOperand offset = new AsmImm12(0);
            /*
            if (dst instanceof AsmVirReg) {
                AsmLw asmLw = new AsmLw(dst, src, offset);
                asmBlock.addInstrTail(asmLw);
            } else {
                AsmFlw asmFLw = new AsmFlw(dst, src, offset);
                asmBlock.addInstrTail(asmFLw);
            }*/
        }

    }

    private AsmOperand parseOperand(Value irValue, int maxImm, Function irFunction, BasicBlock bb) {
        if (operandMap.containsKey(irValue)) {
            AsmOperand asmOperand = operandMap.get(irValue);
            if (((asmOperand instanceof AsmImm32) && maxImm < 32) ||
                    ((asmOperand instanceof AsmImm12) && maxImm < 12)) {
                if ((asmOperand instanceof AsmImm32) && (((AsmImm32) asmOperand).getValue() == 0)) {
                    return ZERO;
                }
                if ((asmOperand instanceof AsmImm12) && (((AsmImm12) asmOperand).getValue() == 0)) {
                    return ZERO;
                }
                //TODO:生成临时寄存器
                AsmOperand tmpReg = genTmpReg(irFunction);
                AsmMove asmMove = new AsmMove(tmpReg, asmOperand);
                blockMap.get(bb).addInstrTail(asmMove);
                return tmpReg;
            }
            return asmOperand;
        }

        //TODO:关于move指令的到时候再写吧
        if (irValue instanceof ConstInt) {
            return parseConstIntOperand(((ConstInt) irValue).getValue(), maxImm, irFunction, bb);
        }
        return null;//待删
    }

    private AsmOperand parseConstIntOperand(int value, int maxImm, Function irFunction, BasicBlock bb) {
        AsmImm32 asmImm32 = new AsmImm32(value);
        AsmImm12 asmImm12 = new AsmImm12(value);
        if (maxImm == 32) {
            return asmImm32;
        }
        if (maxImm == 12 && (value >= -2048 && value <= 2047)) {
            return asmImm12;
        }
        AsmBlock asmBlock = blockMap.get(bb);
        AsmVirReg tmpReg = genTmpReg(irFunction);
        AsmMove asmMove = new AsmMove(tmpReg, asmImm32);
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
