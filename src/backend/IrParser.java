package backend;

import backend.asmInstr.asmBr.AsmBeqz;
import backend.asmInstr.asmBr.AsmJ;
import backend.asmInstr.asmBinary.*;
import backend.asmInstr.asmConv.AsmFtoi;
import backend.asmInstr.asmConv.AsmZext;
import backend.asmInstr.asmConv.AsmitoF;
import backend.asmInstr.asmLS.*;
import backend.asmInstr.asmTermin.AsmCall;
import backend.asmInstr.asmTermin.AsmRet;
import backend.itemStructure.*;
import backend.regs.AsmFVirReg;
import backend.regs.AsmVirReg;
import backend.regs.RegGeter;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.instr.binop.*;
import frontend.ir.instr.convop.ConversionOperation;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.instr.otherop.cmp.Cmp;
import frontend.ir.instr.otherop.cmp.CmpCond;
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

import java.util.*;

import static backend.regs.RegGeter.ZERO;
import static frontend.ir.DataType.*;

public class IrParser {
    private Program program;
    private AsmModule asmModule = new AsmModule();
    //全局变量的c表示映射到全局变量的asm表示。全局变量的llvm表示是标签，对于asm来说没有意义
    private HashMap<Symbol, AsmGlobalVar> gvMap = new HashMap<>();
    private HashMap<Function, AsmFunction> funcMap = new HashMap<>();
    private HashMap<BasicBlock, AsmBlock> blockMap = new HashMap<>();
    //llvm的虚拟寄存器或立即数映射到asm的物理寄存器或立即数
    private HashMap<Value, AsmOperand> operandMap = new HashMap<>();
    //指示对应浮点数值映射到的标签
    private HashMap<Integer, AsmLabel> floatLabelMap = new HashMap<>();
    private HashMap<Map<AsmBlock, Map<AsmOperand, AsmOperand>>, AsmOperand> blockDivExp2Res = new HashMap<>();
    private LibFunctionGeter libFunctionGeter = new LibFunctionGeter();

    public IrParser(Program program) {
        this.program = program;
    }


    public AsmModule parse() {
        parseGlobalVars();
        parseFunctions();
        return asmModule;
    }

    private void parseGlobalVars() {
        List<Symbol> globalVars = program.getGlobalVars();
        for (Symbol globalVar : globalVars) {
            AsmGlobalVar asmGlobalVar = parseGlobalVar(globalVar);
            asmModule.addGlobalVar(asmGlobalVar);
            gvMap.put(globalVar, asmGlobalVar);
        }
    }

    private AsmGlobalVar parseGlobalVar(Symbol globalVar) {
        AsmType type = globalVar.getAsmType();
        if (type == AsmType.INT) {
            boolean isInit = (globalVar.getInitVal() == null) ? false : true;
            if (!isInit)
                return new AsmGlobalVar(globalVar.getName());
            Number initVal = globalVar.getValue();
            return new AsmGlobalVar(globalVar.getName(), initVal);
        }
        if (type == AsmType.FLOAT) {
            boolean isInit = (globalVar.getInitVal() == null) ? false : true;
            if (!isInit)
                return new AsmGlobalVar(globalVar.getName());
            Number floatVal = globalVar.getValue();
            Number initVal = Float.floatToRawIntBits(floatVal.floatValue());
            return new AsmGlobalVar(globalVar.getName(), initVal);
        }
        if (type == AsmType.ARRAY) {
            boolean isInit = (globalVar.getInitVal() == null) ? false : true;
            if (!isInit)
                return new AsmGlobalVar(globalVar.getName(), globalVar.getLimSize() * 4);
            return new AsmGlobalVar(globalVar);
        }
        throw new RuntimeException("全局变量类型错误");
    }


    private void parseFunctions() {
        createMaps();
        for (Function f : program.getFunctions().values()) {
            parseFunction(f);
        }
    }

    private void createMaps() {
        for (Function f : program.getFunctions().values()) {
            AsmFunction asmFunction = new AsmFunction(f.getName());
            asmModule.addFunction(asmFunction);
            funcMap.put(f, asmFunction);
            int blockIndex = 0;
            for (Node bb : f.getBasicBlocks()) {
                //基于新的块编号策略
                AsmBlock asmBlock = new AsmBlock(f.getName() + "_" + ((BasicBlock) bb).value2string());
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
                //TODO:尾递归？
                if (instr instanceof CallInstr) {
                    List rParams = ((CallInstr) instr).getRParams();
                    int size = rParams.size();
                    for (int i = 8; i < size; i++) {
                        if (((Value) rParams.get(i)).getDataType() == INT) {
                            pushArgSize += 4;
                        } else {
                            pushArgSize += 8;
                        }
                    }
                }
            }
        }
        //+8是ra的大小
        asmFunction.setArgsSize(pushArgSize);
        asmFunction.setRaSize(8);

        for (Node bb : f.getBasicBlocks()) {
            parseBlock((BasicBlock) bb, f);
        }

        BasicBlock bb = (BasicBlock) f.getBasicBlocks().getHead();
        List<Value> args = f.getFParamValueList();
        List<Value> iargs = new ArrayList<>();
        List<Value> fargs = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            Value arg = args.get(i);
            if (arg.getPointerLevel() != 0) {
                iargs.add(arg);
            } else if (arg.getDataType() == FLOAT) {
                fargs.add(arg);
            } else {
                iargs.add(arg);
            }
        }
        int iargnum = Math.min(iargs.size(), 8);
        int fargnum = Math.min(fargs.size(), 8);
        for (int i = 0; i < iargnum; i++) {
            Value arg = iargs.get(i);
            AsmOperand asmOperand = parseOperand(arg, 0, f, bb);
            AsmMove asmMove = new AsmMove(asmOperand, RegGeter.AregsInt.get(i));
            blockMap.get(bb).addInstrHead(asmMove);
        }
        for (int i = 0; i < fargnum; i++) {
            Value arg = fargs.get(i);
            AsmOperand asmOperand = parseOperand(arg, 0, f, bb);
            AsmMove asmMove = new AsmMove(asmOperand, RegGeter.AregsFloat.get(i));
            blockMap.get(bb).addInstrHead(asmMove);
        }
        int offset = asmFunction.getWholeSize();
        for (int i = 8; i < iargs.size(); i++) {
            Value arg = iargs.get(i);
            AsmOperand asmOperand = parseOperand(arg, 0, f, bb);
            if (arg.getPointerLevel() != 0) {
                //TODO:暂时不处理栈大小超过2048字节的情况
                AsmLw asmLw = new AsmLw(asmOperand, RegGeter.SP, new AsmImm12(offset));
                blockMap.get(bb).addInstrHead(asmLw);
                offset += 8;
            } else {
                AsmLd asmLd = new AsmLd(asmOperand, RegGeter.SP, new AsmImm12(offset));
                blockMap.get(bb).addInstrHead(asmLd);
                offset += 4;
            }
        }
        for (int i = 8; i < fargs.size(); i++) {
            Value arg = fargs.get(i);
            AsmOperand asmOperand = parseOperand(arg, 0, f, bb);
            AsmFlw asmflw = new AsmFlw(asmOperand, RegGeter.SP, new AsmImm12(offset));
            blockMap.get(bb).addInstrHead(asmflw);
            offset += 4;
        }
        if (asmFunction.getRaSize() != 0) {
            AsmSd asmSd = new AsmSd(RegGeter.RA, RegGeter.SP, new AsmImm12(asmFunction.getWholeSize() - 8));
            blockMap.get(bb).addInstrHead(asmSd);
        }
        AsmSub asmSub = new AsmSub(RegGeter.SP, RegGeter.SP, new AsmImm12(asmFunction.getWholeSize()));
        blockMap.get(bb).addInstrHead(asmSub);
    }

    private void parseBlock(BasicBlock bb, Function f) {
        for (Node instr : bb.getInstructions()) {
            parseInstr((Instruction) instr, f, bb);
        }
    }

    private void parseInstr(Instruction instr, Function f, BasicBlock bb) {
        if (instr instanceof ReturnInstr)
            parseRet((ReturnInstr) instr, bb, f);
        else if (instr instanceof AllocaInstr)
            parseAlloca((AllocaInstr) instr, bb, f);
        else if (instr instanceof LoadInstr)
            parseLoad((LoadInstr) instr, bb, f);
        else if (instr instanceof StoreInstr)
            parseStore((StoreInstr) instr, bb, f);
        else if (instr instanceof Cmp)
            parseCmp((Cmp) instr, bb, f);
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
            parseFDiv((FDivInstr) instr, bb, f);
        else if (instr instanceof SRemInstr)
            parseMod((SRemInstr) instr, bb, f);
        else if (instr instanceof BranchInstr)
            parseBr((BranchInstr) instr, bb, f);
        else if (instr instanceof JumpInstr)
            parseJump((JumpInstr) instr, bb, f);
        else if (instr instanceof CallInstr)
            parseCall((CallInstr) instr, bb, f);
        else if (instr instanceof GEPInstr)
            parseGEP((GEPInstr) instr, bb, f);
            /*else if(instr instanceof Move)*/
        else if (instr instanceof ConversionOperation)
            parseConv((ConversionOperation) instr, bb, f);
    }

    private void parseRet(ReturnInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        Value retValue = instr.getReturnValue();
        if (retValue != null) {
            if (f.getDataType() == INT) {
                AsmOperand asmOperand = parseOperand(retValue, 32, f, bb);
                AsmMove asmMove = new AsmMove(RegGeter.AregsInt.get(0), asmOperand);
                asmBlock.addInstrTail(asmMove);
            } else {
                AsmOperand asmOperand = parseOperand(retValue, 32, f, bb);
                AsmMove asmMove = new AsmMove(RegGeter.AregsFloat.get(0), asmOperand);
                asmBlock.addInstrTail(asmMove);
            }
        }
        AsmFunction asmFunction = funcMap.get(f);
        AsmLd asmLd = new AsmLd(RegGeter.RA, RegGeter.SP, new AsmImm12(asmFunction.getWholeSize() - 8));
        asmBlock.addInstrTail(asmLd);
        AsmAdd asmAdd = new AsmAdd(RegGeter.SP, RegGeter.SP, new AsmImm12(asmFunction.getWholeSize()));
        asmBlock.addInstrTail(asmAdd);
        asmBlock.addInstrTail(new AsmRet());
    }

    private void parseAlloca(AllocaInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmFunction asmFunction = funcMap.get(f);
        AsmType type;
        if (instr.getPointerLevel() != 1) {
            type = AsmType.POINTER;
        } else {
            type = instr.getSymbol().getAsmType();
        }
        int offset = asmFunction.getArgsSize() + asmFunction.getAllocaSize();
        AsmOperand offOp = parseConstIntOperand(offset, 12, f, bb);
        if (type == AsmType.ARRAY) {
            asmFunction.addAllocaSize(4 * instr.getSymbol().getLimSize());
        } else if (type == AsmType.POINTER) {
            asmFunction.addAllocaSize(8);
        } else {
            asmFunction.addAllocaSize(4);
        }
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmAdd asmAdd = new AsmAdd(dst, RegGeter.SP, offOp);
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
            AsmOperand src = parseGlobalToOperand(addr, bb);
            AsmLa asmLa = new AsmLa(laReg, src);
            asmBlock.addInstrTail(asmLa);
            AsmOperand offset = new AsmImm12(0);
            if (dst instanceof AsmVirReg) {
                AsmLw asmLw = new AsmLw(dst, laReg, offset);
                asmBlock.addInstrTail(asmLw);
            } else {
                AsmFlw asmFLw = new AsmFlw(dst, laReg, offset);
                asmBlock.addInstrTail(asmFLw);
            }
        } else if (instr.getPointerLevel() != 0) {
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            AsmOperand src;
            if (instr.getPtr() != null) {
                src = parseOperand(instr.getPtr(), 0, f, bb);
            } else {
                src = parseOperand(addr.getAllocValue(), 0, f, bb);
            }
            AsmOperand offset = new AsmImm12(0);
            AsmLd asmLd = new AsmLd(dst, src, offset);
            asmBlock.addInstrTail(asmLd);
        } else {
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            AsmOperand src;
            if (instr.getPtr() != null) {
                src = parseOperand(instr.getPtr(), 0, f, bb);
            } else {
                src = parseOperand(addr.getAllocValue(), 0, f, bb);
            }
            AsmOperand offset = new AsmImm12(0);
            if (dst instanceof AsmVirReg) {
                AsmLw asmLw = new AsmLw(dst, src, offset);
                asmBlock.addInstrTail(asmLw);
            } else {
                AsmFlw asmFLw = new AsmFlw(dst, src, offset);
                asmBlock.addInstrTail(asmFLw);
            }
        }
    }

    private void parseStore(StoreInstr instr, BasicBlock bb, Function f) {
        //store float %4, float* %5
        //sw s0,0(s1)
        AsmBlock asmBlock = blockMap.get(bb);
        Symbol addr = instr.getSymbol();
        if (addr.isGlobal()) {
            AsmOperand laReg = genTmpReg(f);
            AsmOperand addrOp = parseGlobalToOperand(addr, bb);
            AsmLa asmLa = new AsmLa(laReg, addrOp);
            asmBlock.addInstrTail(asmLa);
            AsmOperand src = parseOperand(instr.getValue(), 0, f, bb);
            AsmOperand offset = new AsmImm12(0);
            if (src instanceof AsmVirReg) {
                AsmSw asmSw = new AsmSw(src, laReg, offset);
                asmBlock.addInstrTail(asmSw);
            } else {
                AsmFsw asmFsw = new AsmFsw(src, laReg, offset);
                asmBlock.addInstrTail(asmFsw);
            }
        } else if (instr.getPointerLevel() != 0) {
            AsmOperand src = parseOperand(instr.getValue(), 0, f, bb);
            AsmOperand dst;
            if (instr.getPtr() != null) {
                dst = parseOperand(instr.getPtr(), 0, f, bb);
            } else {
                dst = parseOperand(addr.getAllocValue(), 0, f, bb);
            }
            AsmOperand offset = new AsmImm12(0);
            AsmSd asmSd = new AsmSd(src, dst, offset);
            asmBlock.addInstrTail(asmSd);
        } else {
            AsmOperand src = parseOperand(instr.getValue(), 0, f, bb);
            AsmOperand dst;
            if (instr.getPtr() != null) {
                dst = parseOperand(instr.getPtr(), 0, f, bb);
            } else {
                dst = parseOperand(addr.getAllocValue(), 0, f, bb);
            }
            AsmOperand offset = new AsmImm12(0);
            if (src instanceof AsmVirReg) {
                AsmSw asmSw = new AsmSw(src, dst, offset);
                asmBlock.addInstrTail(asmSw);
            } else {
                AsmFsw asmFsw = new AsmFsw(src, dst, offset);
                asmBlock.addInstrTail(asmFsw);
            }
        }
    }

    private void parseCmp(Cmp instr, BasicBlock bb, Function f) {
        CmpCond cond = instr.getCond();
        Value op1 = instr.getOp1();
        Value op2 = instr.getOp2();
        boolean isFloat = !(op1.getDataType() == INT && op2.getDataType() == INT);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmBlock asmBlock = blockMap.get(bb);

        if (cond == CmpCond.NE) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 12, f, bb);
                AsmOperand tmp = genTmpReg(f);
                AsmXor asmXor = new AsmXor(tmp, left, right);
                asmBlock.addInstrTail(asmXor);
                AsmSltu asmSltu = new AsmSltu(dst, ZERO, tmp);
                asmBlock.addInstrTail(asmSltu);
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmOperand tmp = genTmpReg(f);
                AsmFeq asmFeq = new AsmFeq(tmp, left, right);
                asmBlock.addInstrTail(asmFeq);
                AsmXor asmXor = new AsmXor(dst, tmp, new AsmImm12(1));
                asmBlock.addInstrTail(asmXor);
            }
        } else if (cond == CmpCond.EQ) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 12, f, bb);
                AsmOperand tmp = genTmpReg(f);
                AsmXor asmXor = new AsmXor(tmp, left, right);
                asmBlock.addInstrTail(asmXor);
                AsmOperand tmp2 = genTmpReg(f);
                AsmSltu asmSltu = new AsmSltu(tmp2, ZERO, tmp);
                asmBlock.addInstrTail(asmSltu);
                AsmXor asmXor2 = new AsmXor(dst, tmp2, new AsmImm12(1));
                asmBlock.addInstrTail(asmXor2);
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmFeq asmFeq = new AsmFeq(dst, left, right);
                asmBlock.addInstrTail(asmFeq);
            }
        } else if (cond == CmpCond.LT) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 12, f, bb);
                if (right instanceof AsmImm12) {
                    AsmSlti asmSlti = new AsmSlti(dst, left, right);
                    asmBlock.addInstrTail(asmSlti);
                } else {
                    AsmSlt asmSlt = new AsmSlt(dst, left, right);
                    asmBlock.addInstrTail(asmSlt);
                }
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmFlt asmFlt = new AsmFlt(dst, left, right);
                asmBlock.addInstrTail(asmFlt);
            }
        } else if (cond == CmpCond.LE) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 12, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmOperand tmp = genTmpReg(f);
                if (left instanceof AsmImm12) {
                    AsmSlti asmSlti = new AsmSlti(tmp, right, left);
                    asmBlock.addInstrTail(asmSlti);
                } else {
                    AsmSlt asmSlt = new AsmSlt(tmp, right, left);
                    asmBlock.addInstrTail(asmSlt);
                }
                AsmXor asmXor = new AsmXor(dst, tmp, new AsmImm12(1));
                asmBlock.addInstrTail(asmXor);
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmFle asmFle = new AsmFle(dst, right, left);
                asmBlock.addInstrTail(asmFle);
            }
        } else if (cond == CmpCond.GT) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 12, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                if (left instanceof AsmImm12) {
                    AsmSlti asmSlti = new AsmSlti(dst, right, left);
                    asmBlock.addInstrTail(asmSlti);
                } else {
                    AsmSlt asmSlt = new AsmSlt(dst, right, left);
                    asmBlock.addInstrTail(asmSlt);
                }
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmFgt asmFgt = new AsmFgt(dst, left, right);
                asmBlock.addInstrTail(asmFgt);
            }
        } else if (cond == CmpCond.GE) {
            if (!isFloat) {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 12, f, bb);
                AsmOperand tmp = genTmpReg(f);
                if (left instanceof AsmImm12) {
                    AsmSlti asmSlti = new AsmSlti(tmp, left, right);
                    asmBlock.addInstrTail(asmSlti);
                } else {
                    AsmSlt asmSlt = new AsmSlt(tmp, left, right);
                    asmBlock.addInstrTail(asmSlt);
                }
                AsmXor asmXor = new AsmXor(dst, tmp, new AsmImm12(1));
                asmBlock.addInstrTail(asmXor);
            } else {
                AsmOperand left = parseOperand(op1, 0, f, bb);
                AsmOperand right = parseOperand(op2, 0, f, bb);
                AsmFge asmFge = new AsmFge(dst, left, right);
                asmBlock.addInstrTail(asmFge);
            }
        }
    }

    //注意add等指令包括加i加w的各类加，打印的时候不一样
    //TODO:isword的理解
    private void parseAdd(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        Value src1 = instr.getOp1();
        Value src2 = instr.getOp2();
        boolean isConst1 = src1 instanceof ConstInt;
        boolean isConst2 = src2 instanceof ConstInt;
        if (isConst1 && isConst2) {
            int value1 = ((ConstInt) src1).getNumber();
            int value2 = ((ConstInt) src2).getNumber();
            AsmOperand imm = new AsmImm32(value1 + value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else if (isConst1) {
            AsmOperand imm = parseOperand(src1, 12, f, bb);
            AsmOperand virReg = parseOperand(src2, 0, f, bb);
            AsmAdd asmAdd = new AsmAdd(dst, virReg, imm);
            asmAdd.isWord = true;
            asmBlock.addInstrTail(asmAdd);
        } else {
            AsmOperand left = parseOperand(src1, 0, f, bb);
            AsmOperand right = parseOperand(src2, 12, f, bb);
            AsmAdd asmAdd = new AsmAdd(dst, left, right);
            asmAdd.isWord = true;
            asmBlock.addInstrTail(asmAdd);
        }
    }

    //乘法指令比较慢，因此考虑了各种情况下的优化方式
    private void parseMul(MulInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        //src2为特定常数时可以进行相应的优化
        if (instr.is64) {
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
            AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
            AsmMul asmMul = new AsmMul(dst, src1, src2);
            asmBlock.addInstrTail(asmMul);
            return;
        }
        boolean isSrc1Const = instr.getOp1() instanceof ConstInt;
        boolean isSrc2Const = instr.getOp2() instanceof ConstInt;
        if (isSrc1Const || isSrc2Const) {
            if (isSrc1Const && !isSrc2Const) {
                instr.swapOp();
            }
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
            int value = ((ConstInt) instr.getOp2()).getNumber();
            if (value == 1) {
                AsmMove asmMove = new AsmMove(dst, src1);
                asmBlock.addInstrTail(asmMove);
            } else if (value == 0) {
                AsmMove asmMove = new AsmMove(dst, ZERO);
                asmBlock.addInstrTail(asmMove);
            } else if (value == -1) {
                AsmSub asmSub = new AsmSub(dst, ZERO, src1);
                asmSub.isWord = true;
                asmBlock.addInstrTail(asmSub);
            } else if (isTwoTimes(Math.abs(value))) {
                int absValue = Math.abs(value);
                int shift = -1;
                while (absValue != 0) {
                    absValue >>= 1;
                    shift++;
                }
                AsmSll asmSll = new AsmSll(dst, src1, new AsmImm12(shift));
                asmBlock.addInstrTail(asmSll);
                if (value < 0) {
                    AsmSub asmSub = new AsmSub(dst, ZERO, dst);
                    asmSub.isWord = true;
                    asmBlock.addInstrTail(asmSub);
                }
            } else if (isTwoTimes(Math.abs(value) + 1)) {
                int absValue = Math.abs(value + 1);
                int shift = -1;
                while (absValue != 0) {
                    absValue >>= 1;
                    shift++;
                }
                AsmOperand tmpReg = genTmpReg(f);
                AsmSll asmSll = new AsmSll(tmpReg, src1, new AsmImm12(shift));
                asmBlock.addInstrTail(asmSll);
                AsmSub asmSub1 = new AsmSub(dst, dst, src1);
                asmSub1.isWord = true;
                asmBlock.addInstrTail(asmSub1);
                if (value < 0) {
                    AsmSub asmSub2 = new AsmSub(dst, ZERO, dst);
                    asmSub2.isWord = true;
                    asmBlock.addInstrTail(asmSub2);
                }
            } else if (false/*TODO:先这样吧，不想再优化了*/) {
                assert true;
            } else {
                //TODO:我认为maxImm是12
                AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
                AsmMul asmMul = new AsmMul(dst, src1, src2);
                asmMul.isWord = true;
                asmBlock.addInstrTail(asmMul);
            }
        } else {
            AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
            AsmMul asmMul = new AsmMul(dst, src1, src2);
            asmMul.isWord = true;
            asmBlock.addInstrTail(asmMul);
        }
    }

    public boolean isTwoTimes(int value) {
        return (value & (value - 1)) == 0;
    }


    private void parseFAdd(FAddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        AsmFAdd asmFAdd = new AsmFAdd(dst, src1, src2);
        asmBlock.addInstrTail(asmFAdd);
    }

    private void parseFMul(FMulInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        AsmFMul asmFMul = new AsmFMul(dst, src1, src2);
        asmBlock.addInstrTail(asmFMul);
    }

    private void parseSub(SubInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        Value src1 = instr.getOp1();
        Value src2 = instr.getOp2();
        boolean isConst1 = src1 instanceof ConstInt;
        boolean isConst2 = src2 instanceof ConstInt;
        //addi,subi都的src1都只能是立即数，但减法不满足交换律，因此少一个分支
        //TODO:减转加的优化
        if (isConst1 && isConst2) {
            int value1 = ((ConstInt) src1).getNumber();
            int value2 = ((ConstInt) src2).getNumber();
            AsmOperand imm = new AsmImm32(value1 - value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else {
            AsmOperand left = parseOperand(src1, 0, f, bb);
            AsmOperand right = parseOperand(src2, 12, f, bb);
            AsmSub asmSub = new AsmSub(dst, left, right);
            asmSub.isWord = true;
            asmBlock.addInstrTail(asmSub);
        }
    }

    private void parseFSub(FSubInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        AsmFSub asmFSub = new AsmFSub(dst, src1, src2);
        asmBlock.addInstrTail(asmFSub);
    }

    private void parseSDiv(SDivInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        if (instr.getOp2() instanceof ConstInt) {
            int value = ((ConstInt) instr.getOp2()).getNumber();
            //不想写Pair,用Map代替
            Map<AsmOperand, AsmOperand> divExp = new HashMap<>();
            divExp.put(src1, new AsmImm32(value));
            Map<AsmBlock, Map<AsmOperand, AsmOperand>> block2Exp = new HashMap<>();
            block2Exp.put(asmBlock, divExp);
            if (blockDivExp2Res.containsKey(block2Exp)) {
                //TODO：不是哥们，怎么没move啊？
                operandMap.put(instr, blockDivExp2Res.get(block2Exp));
            } else {
                divByConst(dst, src1, value, bb, f);
            }
        } else {
            AsmDiv asmDiv = new AsmDiv(dst, src1, src2);
            if (!instr.is64) {
                asmDiv.isWord = true;
            }
            asmBlock.addInstrTail(asmDiv);
        }
    }

    private void divByConst(AsmOperand dst, AsmOperand src1, int value, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        if (value == 1) {
            AsmMove asmMove = new AsmMove(dst, src1);
            asmBlock.addInstrTail(asmMove);
        } else if (value == -1) {
            AsmSub asmSub = new AsmSub(dst, ZERO, src1);
            asmSub.isWord = true;
            asmBlock.addInstrTail(asmSub);
        } else if (/*TODO:这里没有看懂优化*/isTwoTimes(Math.abs(value))) {
            int absValue = Math.abs(value);
            int shift = -1;
            while (absValue != 0) {
                absValue >>= 1;
                shift++;
            }
            AsmSra asmSra = new AsmSra(dst, src1, new AsmImm12(shift));
            asmSra.isWord = true;
            asmBlock.addInstrTail(asmSra);
        } else {
            AsmOperand src2 = new AsmImm32(value);
            AsmDiv asmDiv = new AsmDiv(dst, src1, src2);
            asmDiv.isWord = true;
            asmBlock.addInstrTail(asmDiv);
        }
        if (value < 0) {
            //TODO:试试neg指令
            AsmSub asmSub = new AsmSub(dst, ZERO, dst);
            asmSub.isWord = true;
            asmBlock.addInstrTail(asmSub);
        }
        Map<AsmOperand, AsmOperand> divExp = new HashMap<>();
        divExp.put(src1, new AsmImm32(value));
        Map<AsmBlock, Map<AsmOperand, AsmOperand>> block2Exp = new HashMap<>();
        block2Exp.put(asmBlock, divExp);
        blockDivExp2Res.put(block2Exp, dst);
    }

    private void parseFDiv(FDivInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        AsmFDiv asmFDiv = new AsmFDiv(dst, src1, src2);
        asmBlock.addInstrTail(asmFDiv);
    }

    private void parseMod(SRemInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 0, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 0, f, bb);
        //TODO:注意指令名字叫rem
        AsmMod asmMod = new AsmMod(dst, src1, src2);
        asmMod.isWord = true;
        asmBlock.addInstrTail(asmMod);
    }

    private void parseBr(BranchInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        Value cond = instr.getCond();
        BasicBlock trueBB = instr.getThenTarget();
        BasicBlock falseBB = instr.getElseTarget();
        if (cond instanceof ConstInt) {
            int value = ((ConstInt) cond).getNumber();
            if (value != 0) {
                AsmJ asmJ = new AsmJ(blockMap.get(trueBB));
                asmBlock.addInstrTail(asmJ);
            } else {
                AsmJ asmJ = new AsmJ(blockMap.get(falseBB));
                asmBlock.addInstrTail(asmJ);
            }
        } else {
            //TODO:为什么需要区分CmpInstr?
            AsmOperand cmp = parseOperand(cond, 0, f, bb);
            AsmBlock trueAsmBlock = blockMap.get(trueBB);
            AsmBlock falseAsmBlock = blockMap.get(falseBB);
            AsmBeqz asmBeqz = new AsmBeqz(cmp, trueAsmBlock);
            asmBlock.addInstrTail(asmBeqz);
            AsmJ asmJ = new AsmJ(falseAsmBlock);
            asmBlock.addInstrTail(asmJ);
            //TODO:感觉不需要setTrueBlock和setFalseBlock?
        }
    }

    private void parseJump(JumpInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        BasicBlock target = instr.getTarget();
        AsmJ asmJ = new AsmJ(blockMap.get(target));
        asmBlock.addInstrTail(asmJ);
        //TODO:感觉不需要setTrueBlock和setFalseBlock?
    }

    private void parseCall(CallInstr instr, BasicBlock bb, Function f) {
        AsmFunction asmFunction = funcMap.get(f);
        AsmBlock asmBlock = blockMap.get(bb);
        //TODO:库函数的处理
        List<Value> args = instr.getRParams();
        List<Value> floatArgs = new ArrayList<>();
        List<Value> intArgs = new ArrayList<>();
        for (Value arg : args) {
            if (arg.getDataType() == FLOAT) {
                floatArgs.add(arg);
            } else {
                intArgs.add(arg);
            }
        }
        int intArgNum = intArgs.size();
        int floatArgNum = floatArgs.size();
        int intArgRegNum = Math.min(intArgNum, 8);
        int floatArgRegNum = Math.min(floatArgNum, 8);
        for (int i = 0; i < intArgRegNum; i++) {
            AsmOperand argReg = parseOperand(intArgs.get(i), 12, f, bb);
            AsmMove asmMove = new AsmMove(RegGeter.AregsInt.get(i), argReg);
            asmBlock.addInstrTail(asmMove);
        }
        for (int i = 0; i < floatArgRegNum; i++) {
            AsmOperand argReg = parseOperand(floatArgs.get(i), 12, f, bb);
            AsmMove asmMove = new AsmMove(RegGeter.AllRegsFloat.get(i), argReg);
            asmBlock.addInstrTail(asmMove);
        }
        if (false/*TODO:尾递归*/) {
            assert true;
        } else {
            int offset = 0;
            for (int i = 8; i < intArgs.size(); i++) {
                Value arg = intArgs.get(i);
                AsmOperand argReg = parseOperand(arg, 12, f, bb);
                if (argReg instanceof AsmImm12) {
                    AsmOperand tmpReg = genTmpReg(f);
                    AsmMove asmMove = new AsmMove(tmpReg, argReg);
                    asmBlock.addInstrTail(asmMove);
                    argReg = tmpReg;
                }
                //TODO:偏移量大于2048
                if (arg.getDataType() == INT) {
                    AsmSw asmSw = new AsmSw(argReg, RegGeter.SP, new AsmImm12(offset));
                    asmBlock.addInstrTail(asmSw);
                    offset += 4;
                } else {
                    AsmSd asmSd = new AsmSd(argReg, RegGeter.SP, new AsmImm12(offset));
                    asmBlock.addInstrTail(asmSd);
                    offset += 8;
                }
            }
            for (int i = 8; i < floatArgs.size(); i++) {
                AsmOperand argReg = parseOperand(intArgs.get(i), 12, f, bb);
                if (argReg instanceof AsmImm12) {
                    AsmOperand tmpReg = genFloatTmpReg(f);
                    AsmMove asmMove = new AsmMove(tmpReg, argReg);
                    asmBlock.addInstrTail(asmMove);
                    argReg = tmpReg;
                }
                AsmFsw asmFsw = new AsmFsw(argReg, RegGeter.SP, new AsmImm12(offset));
                asmBlock.addInstrTail(asmFsw);
                offset += 4;
            }
        }
        AsmCall asmCall = new AsmCall(instr.getFuncDef().getName());
        asmBlock.addInstrTail(asmCall);
        if (instr.getDataType() != VOID) {
            AsmOperand dst = parseOperand(instr, 0, f, bb);
            if (instr.getDataType() == INT) {
                AsmMove asmMove = new AsmMove(dst, RegGeter.AregsInt.get(0));
                asmBlock.addInstrTail(asmMove);
            } else {
                AsmMove asmMove = new AsmMove(dst, RegGeter.AllRegsFloat.get(0));
                asmBlock.addInstrTail(asmMove);
            }
        }
    }


    //TODO:parseGep
    /*private void parseGep(GEPInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        Value ptr = instr.getPtr();
        List<Value> indices = instr.getIndices();
        AsmOperand base = parseOperand(ptr, 0, f, bb);
        int offset = 0;
        for (int i = 0; i < indices.size(); i++) {
            Value index = indices.get(i);
            AsmOperand indexReg = parseOperand(index, 12, f, bb);
            if (indexReg instanceof AsmImm12) {
                AsmOperand tmpReg = genTmpReg(f);
                AsmMove asmMove = new AsmMove(tmpReg, indexReg);
                asmBlock.addInstrTail(asmMove);
                indexReg = tmpReg;
            }
            AsmSlli asmSlli = new AsmSlli(indexReg, indexReg, new AsmImm12(2));
            asmBlock.addInstrTail(asmSlli);
            AsmAdd asmAdd = new AsmAdd(base, base, indexReg);
            asmAdd.isWord = true;
            asmBlock.addInstrTail(asmAdd);
        }
        AsmMove asmMove = new AsmMove(dst, base);
        asmBlock.addInstrTail(asmMove);
    }*/

    //TODO:parseMove
    /*private void parseMove(MoveInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src = parseOperand(instr.getOp(), 0, f, bb);
        AsmMove asmMove = new AsmMove(dst, src);
        asmBlock.addInstrTail(asmMove);
    }*/

    private void parseConv(ConversionOperation instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src = parseOperand(instr.getValue(), 0, f, bb);
        if (instr.getOpName().equals("fptosi")) {
            AsmFtoi asmFtoi = new AsmFtoi(dst, src);
            asmBlock.addInstrTail(asmFtoi);
        } else if (instr.getOpName().equals("sitofp")) {
            AsmitoF asmitoF = new AsmitoF(dst, src);
            asmBlock.addInstrTail(asmitoF);
        } else if (instr.getOpName().equals("zext")) {
            AsmZext asmZext = new AsmZext(dst, src);
            asmBlock.addInstrTail(asmZext);
        } else {
            //TODO:bitcast的实现
        }
    }

    private void parseGEP(GEPInstr instr, BasicBlock bb, Function f) {
        AsmFunction asmFunction = funcMap.get(f);
        AsmBlock asmBlock = blockMap.get(bb);
        List<Value> indexList = instr.getIndexList();
        AsmOperand base = parseOperand(instr.getPtrVal(), 0, f, bb);
        AsmOperand result = parseOperand(instr, 0, f, bb);
        AsmMove asmMove = new AsmMove(result, base);
        asmBlock.addInstrTail(asmMove);
        List<Integer> sizeList = instr.getSizeList();
        int offset = 0;
        for (int i = 0; i < sizeList.size(); i++) {
            if (indexList.get(i) instanceof ConstInt) {
                int index = ((ConstInt) indexList.get(i)).getNumber();
                offset = index * sizeList.get(i) * 4;
                if (offset != 0) {
                    AsmAdd asmAdd = new AsmAdd(result, result, parseConstIntOperand(offset, 12, f, bb));
                    asmBlock.addInstrTail(asmAdd);
                }
            } else {
                AsmOperand index = parseOperand(indexList.get(i), 0, f, bb);
                AsmMul asmMul = new AsmMul(index, index, parseConstIntOperand(sizeList.get(i) * 4, 12, f, bb));
                asmBlock.addInstrTail(asmMul);
                AsmAdd asmAdd = new AsmAdd(result, result, index);
                asmBlock.addInstrTail(asmAdd);
            }
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
                AsmOperand tmpReg = genTmpReg(irFunction);
                AsmMove asmMove = new AsmMove(tmpReg, asmOperand);
                blockMap.get(bb).addInstrTail(asmMove);
                return tmpReg;
            }
            return asmOperand;
        }

        //TODO:关于move指令的到时候再写吧

        if (irValue instanceof ConstInt) {
            return parseConstIntOperand(((ConstInt) irValue).getNumber(), maxImm, irFunction, bb);
        }
        if (irValue instanceof ConstFloat) {
            return parseConstFloatOperand(((ConstFloat) irValue).getNumber(), maxImm, irFunction, bb);
        }
        //全局变量的解析应该只会在load和store指令中出现,单独完成吧
        AsmFunction asmFunction = funcMap.get(irFunction);
        if (irValue.getDataType() == FLOAT) {
            AsmFVirReg tmpReg = new AsmFVirReg();
            //TODO:这里是addUsedVirReg还是addUsedFVirReg
            asmFunction.addUsedVirReg(tmpReg);
            if (!(irValue instanceof ConstFloat)) {
                //TODO:operandMap1也要加入键值对？
                operandMap.put(irValue, tmpReg);
            }
            return tmpReg;
        }
        AsmVirReg tmpReg = genTmpReg(irFunction);
        if (!(irValue instanceof ConstInt)) {
            //TODO:operandMap1也要加入键值对？
            operandMap.put(irValue, tmpReg);
        }
        return tmpReg;
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

    private AsmFVirReg genFloatTmpReg(Function irFunction) {
        AsmFunction asmFunction = funcMap.get(irFunction);
        AsmFVirReg tmpReg = new AsmFVirReg();
        asmFunction.addUsedVirReg(tmpReg);
        return tmpReg;
    }

    private AsmOperand parseConstFloatOperand(float value, int maxImm, Function irFunction, BasicBlock bb) {
        float floatVal = value;
        int intVal = Float.floatToRawIntBits(floatVal);
        AsmFunction asmFunction = funcMap.get(irFunction);
        AsmFVirReg tmpReg = new AsmFVirReg();
        //TODO:这里是addUsedVirReg还是addUsedFVirReg
        asmFunction.addUsedVirReg(tmpReg);
        AsmOperand label = getFloatLabel(intVal);
        AsmOperand tmpIntReg = genTmpReg(irFunction);
        //TODO:全局变量怎么加载局部地址？
        AsmLa asmLa = new AsmLa(tmpIntReg, label);
        blockMap.get(bb).addInstrTail(asmLa);
        AsmFlw asmFlw = new AsmFlw(tmpReg, tmpIntReg, new AsmImm12(0));
        blockMap.get(bb).addInstrTail(asmFlw);
        return tmpReg;
    }

    private AsmOperand getFloatLabel(int value) {
        if (floatLabelMap.containsKey(value)) {
            return floatLabelMap.get(value);
        }
        AsmGlobalVar asmGlobalVar = new AsmGlobalVar(value);
        asmModule.addGlobalVar(asmGlobalVar);
        AsmLabel asmLabel = new AsmLabel(asmGlobalVar.name);
        floatLabelMap.put(value, asmLabel);
        return asmLabel;
    }

    private AsmOperand parseGlobalToOperand(Symbol symbol, BasicBlock bb) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmGlobalVar asmGlobalVar = gvMap.get(symbol);
        AsmLabel asmLabel = new AsmLabel(asmGlobalVar.name);
        return asmLabel;
    }


    private void parseAnd(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 12, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
        if ((src1 instanceof AsmImm12) && (src2 instanceof AsmImm12)) {
            int value1 = ((AsmImm12) src1).getValue();
            int value2 = ((AsmImm12) src2).getValue();
            AsmOperand imm = new AsmImm32(value1 & value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else if (src1 instanceof AsmImm12) {
            AsmAnd asmAnd = new AsmAnd(dst, src2, src1);
            asmBlock.addInstrTail(asmAnd);
        } else {
            AsmAnd asmAnd = new AsmAnd(dst, src1, src2);
            asmBlock.addInstrTail(asmAnd);
        }
    }

    private void parseOr(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 12, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
        if ((src1 instanceof AsmImm12) && (src2 instanceof AsmImm12)) {
            int value1 = ((AsmImm12) src1).getValue();
            int value2 = ((AsmImm12) src2).getValue();
            AsmOperand imm = new AsmImm32(value1 | value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else if (src1 instanceof AsmImm12) {
            AsmOr asmOr = new AsmOr(dst, src2, src1);
            asmBlock.addInstrTail(asmOr);
        } else {
            AsmOr asmOr = new AsmOr(dst, src1, src2);
            asmBlock.addInstrTail(asmOr);
        }
    }

    private void parseXor(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 12, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
        if ((src1 instanceof AsmImm12) && (src2 instanceof AsmImm12)) {
            int value1 = ((AsmImm12) src1).getValue();
            int value2 = ((AsmImm12) src2).getValue();
            AsmOperand imm = new AsmImm32(value1 ^ value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else if (src1 instanceof AsmImm12) {
            AsmXor asmXor = new AsmXor(dst, src2, src1);
            asmBlock.addInstrTail(asmXor);
        } else {
            AsmXor asmXor = new AsmXor(dst, src1, src2);
            asmBlock.addInstrTail(asmXor);
        }
    }

    private void parseShl(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 12, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
        if ((src1 instanceof AsmImm12) && (src2 instanceof AsmImm12)) {
            int value1 = ((AsmImm12) src1).getValue();
            int value2 = ((AsmImm12) src2).getValue();
            AsmOperand imm = new AsmImm32(value1 << value2);
            AsmMove asmMove = new AsmMove(dst, imm);
            asmBlock.addInstrTail(asmMove);
        } else if (src1 instanceof AsmImm12) {
            AsmOperand tmpReg = genTmpReg(f);
            AsmAdd asmAdd = new AsmAdd(tmpReg, ZERO, src1);
            asmBlock.addInstrTail(asmAdd);
            AsmSll asmSll = new AsmSll(dst, tmpReg, src2);
            asmBlock.addInstrTail(asmSll);
        } else {
            AsmSll asmSll = new AsmSll(dst, src1, src2);
            asmBlock.addInstrTail(asmSll);
        }
    }

    private void parseShr(AddInstr instr, BasicBlock bb, Function f) {
        AsmBlock asmBlock = blockMap.get(bb);
        AsmOperand dst = parseOperand(instr, 0, f, bb);
        AsmOperand src1 = parseOperand(instr.getOp1(), 12, f, bb);
        AsmOperand src2 = parseOperand(instr.getOp2(), 12, f, bb);
        if ((src1 instanceof AsmImm12) && (src2 instanceof AsmImm12)) {
            int value1 = ((AsmImm12) src1).getValue();
            int value2 = ((AsmImm12) src2).getValue();
            AsmOperand imm = new AsmImm32(value1 >>> value2);
            AsmAdd asmAdd = new AsmAdd(dst, ZERO, imm);
            asmBlock.addInstrTail(asmAdd);
        } else if (src1 instanceof AsmImm12) {
            AsmOperand tmpReg = genTmpReg(f);
            AsmAdd asmAdd = new AsmAdd(tmpReg, ZERO, src1);
            asmBlock.addInstrTail(asmAdd);
            AsmSrl asmSrl = new AsmSrl(dst, tmpReg, src2);
            asmBlock.addInstrTail(asmSrl);
        } else {
            AsmSrl asmSrl = new AsmSrl(dst, src1, src2);
            asmBlock.addInstrTail(asmSrl);
        }
    }
}
