package backend;

import backend.asmInstr.AsmInstr;
import backend.asmInstr.asmBinary.AsmAdd;
import backend.asmInstr.asmLS.*;
import backend.asmInstr.asmTermin.AsmCall;
import backend.itemStructure.*;
import backend.regs.*;
import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;

import java.util.*;
import java.util.stream.Collectors;
/*todo*/ //load指令偏移量超过32位

public class RegAlloc {
    private RegAlloc(HashMap<AsmOperand, Value> downOperandMap) {
        // 初始化逻辑
        this.downOperandMap = downOperandMap;
    }

    private HashMap<AsmOperand, Value> downOperandMap;
    // 单例模式下的私有静态成员变量
    private static RegAlloc instance = null;

    // 提供一个公共的静态方法，用于获取单例对象
    public static RegAlloc getInstance(HashMap<AsmOperand, Value> downOperandMap) {
        if (instance == null) {
            instance = new RegAlloc(downOperandMap);
        }
        return instance;
    }

    private int FI = 0;//0代表处理Int型，1代表处理Float型
    private static int K = 27;

    HashMap<AsmOperand, Integer> loopDepths = new HashMap<>();
    private HashSet<AsmOperand> S = new HashSet<>();//暂时未用到
    private HashSet<AsmOperand> T = new HashSet<>();

    private HashMap<AsmReg, Integer> preColored = new HashMap<>();

    private HashMap<AsmOperand, HashSet<AsmOperand>> adjList = new HashMap<>();
    private HashMap<AsmOperand, HashSet<AsmOperand>> adjSet = new HashMap<>();
    private HashMap<AsmOperand, Integer> degree = new HashMap<>();
    private HashMap<AsmOperand, HashSet<AsmInstr>> moveList = new HashMap<>();//与该结点相关的传送指令集合

    private HashMap<AsmOperand, AsmOperand> alias = new HashMap<>();//传送指令(u,v)已被合并，v已经放到已合并结点集合，alias(v) = u

    private HashMap<AsmOperand, Integer> color = new HashMap<>();
    //@1
    private HashSet<AsmOperand> all = new HashSet<>(); // 临时寄存器集合
    private HashSet<AsmOperand> simplifyWorklist = new HashSet<>();//低度数的传送无关的节点
    private HashSet<AsmOperand> freezeWorkList = new HashSet<>();//低度数的传送有关的指令
    private HashSet<AsmOperand> spillWorkList = new HashSet<>();//高度数节点
    private HashSet<AsmOperand> spilledNodes = new HashSet<>();//本轮中要被溢出的结点集合
    private HashSet<AsmOperand> coalescedNodes = new HashSet<>();//已合并的的传送指令集合
    private HashSet<AsmOperand> coloredNodes = new HashSet<>();//已经成功着色的结点集合

    private Stack<AsmOperand> selectStack = new Stack<>();//一个包含从图中删除的临时变量的栈
    //@1 这部分，结点只能存在于其中一张表
    //@2
    private HashSet<AsmInstr> coalescedMoves = new HashSet<>();//已经合并的传送指令集合
    private HashSet<AsmInstr> constrainedMoves = new HashSet<>();//源操作数和目标操作数冲突的传送指令集合
    private HashSet<AsmInstr> frozenMoves = new HashSet<>();//不再考虑合并的传送指令
    private HashSet<AsmInstr> worklistMoves = new HashSet<>();//有可能合并的传送指令
    private HashSet<AsmInstr> activeMoves = new HashSet<>();//还未做好合并准备的传送指令集合
    //@2 传送指令集合，传送指令只能存在在其中一张表中

    public void run(AsmModule module) {
        PreColor();
        for (AsmFunction function : module.getFunctions()) {
            int addOffSet = 0;
            if (((AsmBlock) function.getBlocks().getHead()).getInstrs().getSize() == 0) {
                continue;
            }
            K = 32;
            FI = 0;
            while (true) {
                initial();
                LivenessAnalysis(function);
                findLoopDepth(function);
                build(function);
                makeWorkList();
                while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
                        !freezeWorkList.isEmpty() || !spillWorkList.isEmpty()) {
                    if (!simplifyWorklist.isEmpty()) {
                        simplify();
                    } else if (!worklistMoves.isEmpty()) {
                        Coalesce();
                    } else if (!freezeWorkList.isEmpty()) {
                        Freeze();
                    } else if (!spillWorkList.isEmpty()) {
                        SelectSpill();
                    }
                }
                AssignColors();
                if (spilledNodes.isEmpty()) {
                    for (AsmOperand vreg : color.keySet()) {
                        AsmVirReg vreg1 = (AsmVirReg) vreg;
                        vreg1.color = color.get(vreg);
                    }
                    //不确定是否要这样
                    addOffSet += callerSave(function);
                    addOffSet += calleeSave(function);
                    allocRealReg(function);
                    break;
                } else {
                    addOffSet += RewriteProgram(function);
                }
            }

            K = 64;
            FI = 1;

            while (true) {
                initial();
                LivenessAnalysis(function);
                findLoopDepth(function);
                build(function);
                makeWorkList();
                while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() ||
                        !freezeWorkList.isEmpty() || !spillWorkList.isEmpty()) {
                    if (!simplifyWorklist.isEmpty()) {
                        simplify();
                    } else if (!worklistMoves.isEmpty()) {
                        Coalesce();
                    } else if (!freezeWorkList.isEmpty()) {
                        Freeze();
                    } else if (!spillWorkList.isEmpty()) {
                        SelectSpill();
                    }
                }
                AssignColors();
                if (spilledNodes.isEmpty()) {
                    for (AsmOperand vreg : color.keySet()) {
                        AsmFVirReg vreg1 = (AsmFVirReg) vreg;
                        vreg1.color = color.get(vreg);
                    }
                    //LivenessAnalysis(function);//不确定是否要这样//删了

                    addOffSet += callerSave(function);
                    addOffSet += calleeSave(function);
                    allocRealReg(function);
                    break;
                } else {
                    addOffSet += RewriteProgram(function);
                }
            }
            allocAndRecycleSP(function);
            changeOffset(function, addOffSet);
            //deleteMove(function);
        }

    }

    private void deleteMove(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (instrHead != null) {
                instrHead = (AsmInstr) instrHead.getNext();
                if (instrHead instanceof AsmMove) {
                    if (((AsmMove) instrHead).getDst() == ((AsmMove) instrHead).getSrc()) {
                        instrHead.removeFromList();
                    }
                }
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void changeOffset(AsmFunction function, int addOffSet) {
        AsmBlock firstBlock = (AsmBlock) function.getBlocks().getHead();
        AsmInstr firstInstr = (AsmInstr) firstBlock.getInstrs().getHead();
        int stackAddOffset = (addOffSet % 8 == 0) ? addOffSet : addOffSet + 4;
        while (firstInstr != null) {
            if (firstInstr instanceof AsmL) {
                if (firstInstr instanceof AsmLw) {
                    if (((AsmLw) firstInstr).isPassIarg == 1) {
                        int originOffset = ((AsmImm32) (((AsmLw) firstInstr).getOffset())).getValue();
                        int newOffset = stackAddOffset + originOffset;
                        FI = 0;
                        storeOrLoadFromMemory(newOffset, (AsmReg) (((AsmL) firstInstr).getDst()), firstInstr, "load", 1, 0);
                        firstInstr.removeFromList();
                    }
                }
                if (firstInstr instanceof AsmLd) {
                    if (((AsmLd) firstInstr).isPassIarg == 1) {
                        int originOffset = ((AsmImm32) (((AsmLd) firstInstr).getOffset())).getValue();
                        int newOffset = stackAddOffset + originOffset;
                        FI = 0;
                        storeDOrLoadDFromMemory(newOffset, (AsmReg) (((AsmL) firstInstr).getDst()), firstInstr, "load", 1, 0);
                        firstInstr.removeFromList();
                    }
                }
                if (firstInstr instanceof AsmFlw) {
                    if (((AsmFlw) firstInstr).isPassIarg == 1) {
                        int originOffset = ((AsmImm32) (((AsmFlw) firstInstr).getOffset())).getValue();
                        int newOffset = stackAddOffset + originOffset;
                        FI = 1;
                        storeOrLoadFromMemory(newOffset, (AsmReg) (((AsmL) firstInstr).getDst()), firstInstr, "load", 1, 0);
                        firstInstr.removeFromList();
                    }
                }
            }
            firstInstr = (AsmInstr) firstInstr.getNext();
        }
    }

    private void allocAndRecycleSP(AsmFunction function) {
        int offset = 0;
        offset = function.getWholeSize() - 8;
        if (function.getRaSize() != 0) {
            if (offset >= -2048 && offset <= 2047) {
                AsmSd asmSd = new AsmSd(RegGeter.RA, RegGeter.SP, new AsmImm12(offset));
                ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmSd);
            } else {
                AsmReg tmpMove = RegGeter.AllRegsInt.get(5);
                AsmMove asmMove = new AsmMove(tmpMove, new AsmImm32(offset));
                AsmOperand tmpAdd = RegGeter.AllRegsInt.get(5);
                AsmAdd asmAdd = new AsmAdd(tmpAdd, RegGeter.SP, tmpMove);
                AsmSd asmSd = new AsmSd(RegGeter.RA, tmpAdd, new AsmImm12(0));
                ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmSd);
                ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmAdd);
                ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmMove);
            }
        }
        offset = -function.getWholeSize();
        if (offset >= -2048 && offset <= 2047) {
            AsmAdd asmAdd = new AsmAdd(RegGeter.SP, RegGeter.SP, new AsmImm12(offset));
            ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmAdd);
        } else {
            AsmOperand tmpMove = RegGeter.AllRegsInt.get(5);
            AsmMove asmMove = new AsmMove(tmpMove, new AsmImm32(offset));
            AsmAdd asmAdd = new AsmAdd(RegGeter.SP, RegGeter.SP, tmpMove);
            ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmAdd);
            ((AsmBlock) function.getBlocks().getHead()).addInstrHead(asmMove);
        }

        offset = function.getWholeSize() - 8;
        if (function.getRaSize() != 0) {
            if (offset >= -2048 && offset <= 2047) {
                AsmLd asmLd = new AsmLd(RegGeter.RA, RegGeter.SP, new AsmImm12(offset));
                asmLd.insertBefore(function.getTailBlock().getInstrTail());
            } else {
                AsmOperand tmpMove = RegGeter.AllRegsInt.get(5);
                AsmMove asmMove = new AsmMove(tmpMove, new AsmImm32(offset));
                AsmOperand tmpAdd = RegGeter.AllRegsInt.get(5);
                AsmAdd asmAdd = new AsmAdd(tmpAdd, RegGeter.SP, tmpMove);
                AsmLd asmLd = new AsmLd(RegGeter.RA, tmpAdd, new AsmImm12(0));
                asmMove.insertBefore(function.getTailBlock().getInstrTail());
                asmAdd.insertBefore(function.getTailBlock().getInstrTail());
                asmLd.insertBefore(function.getTailBlock().getInstrTail());

            }
            AsmAdd asmAddd = new AsmAdd(RegGeter.SP, RegGeter.SP, parseConstIntOperand(function.getWholeSize(), 12, function));
            asmAddd.insertBefore(function.getTailBlock().getInstrTail());
        }
    }

    private AsmOperand parseConstIntOperand(int value, int maxImm, AsmFunction function) {
        AsmImm32 asmImm32 = new AsmImm32(value);
        AsmImm12 asmImm12 = new AsmImm12(value);
        if (maxImm == 32) {
            return asmImm32;
        }
        if (maxImm == 12 && (value >= -2048 && value <= 2047)) {
            return asmImm12;
        }
        AsmOperand tmpReg = RegGeter.AllRegsInt.get(5);
        AsmMove asmMove = new AsmMove(tmpReg, asmImm32);
        asmMove.insertBefore(function.getTailBlock().getInstrTail());
        return tmpReg;
    }

    private void changeAllocSize(AsmFunction function, int newAllocSize) {
        //修改sp
//        ((AsmAdd)((AsmBlock)function.getBlocks().getHead()).getInstrs().getHead()).allocNewSize(newAllocSize);
//        ((AsmAdd)(function.getTailBlock()).getInstrTail().getPrev()).allocNewSize(newAllocSize);
//        //修改ld ra
//        ((AsmS)((AsmBlock)function.getBlocks().getHead()).getInstrs().getHead().getNext()).allocNewSize(newAllocSize);
//        ((AsmL)(function.getTailBlock()).getInstrTail().getPrev().getPrev()).allocNewSize(newAllocSize);
    }
    //调用规约的完成是假定我们已经成功实现活跃性分析的基础上的，此阶段的调用规约我们先只实现整数寄存器的
    //callerSave的寄存器有x1(ra),x5-7(t0-2),x10-11(a0-a1),x12-17(a2-7),x28-31(t3-6)

    private void storeOrLoadFromMemory(int spillPlace, AsmReg reg, AsmInstr nowInstr, String type, int foreOrBack, int needVir) {
        if (spillPlace >= -2048 && spillPlace <= 2047) {
            AsmImm12 place = new AsmImm12(spillPlace);
            if (type == "store") {
                if (foreOrBack == 0) {
                    if (FI == 0) {
                        AsmSw store = new AsmSw(reg, RegGeter.SP, place);
                        store.insertBefore(nowInstr);
                    } else {
                        AsmFsw store = new AsmFsw(reg, RegGeter.SP, place);
                        store.insertBefore(nowInstr);
                    }
                } else {
                    if (FI == 0) {
                        AsmSw store = new AsmSw(reg, RegGeter.SP, place);
                        store.insertAfter(nowInstr);
                    } else {
                        AsmFsw store = new AsmFsw(reg, RegGeter.SP, place);
                        store.insertAfter(nowInstr);
                    }
                }
            } else if (type == "load") {
                if (foreOrBack == 0) {
                    if (FI == 0) {
                        AsmLw load = new AsmLw(reg, RegGeter.SP, place);
                        load.insertBefore(nowInstr);
                    } else {
                        AsmFlw load = new AsmFlw(reg, RegGeter.SP, place);
                        load.insertBefore(nowInstr);
                    }
                } else {
                    if (FI == 0) {
                        AsmLw load = new AsmLw(reg, RegGeter.SP, place);
                        load.insertAfter(nowInstr);
                    } else {
                        AsmFlw load = new AsmFlw(reg, RegGeter.SP, place);
                        load.insertAfter(nowInstr);
                    }
                }
            }
        } else {
            AsmReg tmpMove = null;
            AsmReg tmpAdd = null;
            if (needVir == 0) {
                tmpMove = RegGeter.AllRegsInt.get(5);
                tmpAdd = RegGeter.AllRegsInt.get(5);
            } else {
                tmpMove = new AsmVirReg();
                tmpAdd = tmpMove;
            }
            AsmMove asmMove = new AsmMove(tmpMove, new AsmImm32(spillPlace));
            AsmAdd asmAdd = new AsmAdd(tmpAdd, RegGeter.SP, tmpMove);
            if (foreOrBack == 0) {
                asmMove.insertBefore(nowInstr);
                asmAdd.insertBefore(nowInstr);
                if (type == "load") {
                    if (FI == 0) {
                        AsmLw load = new AsmLw(reg, tmpAdd, new AsmImm12(0));
                        load.insertBefore(nowInstr);
                    } else {
                        AsmFlw load = new AsmFlw(reg, tmpAdd, new AsmImm12(0));
                        load.insertBefore(nowInstr);
                    }
                } else if (type == "store") {
                    if (FI == 0) {
                        AsmSw store = new AsmSw(reg, tmpAdd, new AsmImm12(0));
                        store.insertBefore(nowInstr);
                    } else {
                        AsmFsw store = new AsmFsw(reg, tmpAdd, new AsmImm12(0));
                        store.insertBefore(nowInstr);
                    }
                }
            } else {
                if (type == "load") {
                    if (FI == 0) {
                        AsmLw load = new AsmLw(reg, tmpAdd, new AsmImm12(0));
                        load.insertAfter(nowInstr);
                    } else {
                        AsmFlw load = new AsmFlw(reg, tmpAdd, new AsmImm12(0));
                        load.insertAfter(nowInstr);
                    }
                } else if (type == "store") {
                    if (FI == 0) {
                        AsmSw store = new AsmSw(reg, tmpAdd, new AsmImm12(0));
                        store.insertAfter(nowInstr);
                    } else {
                        AsmFsw store = new AsmFsw(reg, tmpAdd, new AsmImm12(0));
                        store.insertAfter(nowInstr);
                    }
                }
                asmAdd.insertAfter(nowInstr);
                asmMove.insertAfter(nowInstr);
            }
        }


    }

    private void storeDOrLoadDFromMemory(int spillPlace, AsmReg reg, AsmInstr nowInstr, String type, int foreOrBack, int needVir) {
        if (spillPlace >= -2048 && spillPlace <= 2047) {
            AsmImm12 place = new AsmImm12(spillPlace);
            if (type == "store") {
                if (foreOrBack == 0) {
                    if (FI == 0) {
                        AsmSd store = new AsmSd(reg, RegGeter.SP, place);
                        store.insertBefore(nowInstr);
                    }
                } else {
                    if (FI == 0) {
                        AsmSd store = new AsmSd(reg, RegGeter.SP, place);
                        store.insertAfter(nowInstr);
                    }
                }
            } else if (type == "load") {
                if (foreOrBack == 0) {
                    if (FI == 0) {
                        AsmLd load = new AsmLd(reg, RegGeter.SP, place);
                        load.insertBefore(nowInstr);
                    }
                } else {
                    if (FI == 0) {
                        AsmLd load = new AsmLd(reg, RegGeter.SP, place);
                        load.insertAfter(nowInstr);
                    }
                }
            }
        } else {
            AsmReg tmpMove = null;
            AsmReg tmpAdd = null;
            if (needVir == 0) {
                tmpMove = RegGeter.AllRegsInt.get(5);
                tmpAdd = RegGeter.AllRegsInt.get(5);
            } else {
                tmpMove = new AsmVirReg();
                tmpAdd = tmpMove;
            }
            AsmMove asmMove = new AsmMove(tmpMove, new AsmImm32(spillPlace));
            AsmAdd asmAdd = new AsmAdd(tmpAdd, RegGeter.SP, tmpMove);
            if (foreOrBack == 0) {
                asmMove.insertBefore(nowInstr);
                asmAdd.insertBefore(nowInstr);
                if (type == "load") {
                    if (FI == 0) {
                        AsmLd load = new AsmLd(reg, tmpAdd, new AsmImm12(0));
                        load.insertBefore(nowInstr);
                    }
                } else if (type == "store") {
                    if (FI == 0) {
                        AsmSd store = new AsmSd(reg, tmpAdd, new AsmImm12(0));
                        store.insertBefore(nowInstr);
                    }
                }
            } else {
                if (type == "load") {
                    if (FI == 0) {
                        AsmLd load = new AsmLd(reg, tmpAdd, new AsmImm12(0));
                        load.insertAfter(nowInstr);
                    }
                } else if (type == "store") {
                    if (FI == 0) {
                        AsmSd store = new AsmSd(reg, tmpAdd, new AsmImm12(0));
                        store.insertAfter(nowInstr);
                    }
                }
                asmAdd.insertAfter(nowInstr);
                asmMove.insertAfter(nowInstr);
            }
        }


    }

    private int callerSave(AsmFunction function) {
        int newAllocSize = 0;
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (instrHead != null) {
                if (instrHead instanceof AsmCall) {
                    AsmCall call = (AsmCall) instrHead;
                    for (AsmReg save : call.LiveOut) {
                        int beColored = 0;
                        if (FI == 0) {
                            if (save instanceof AsmVirReg) {
                                beColored = color.get(save);
                            }
                            if (save instanceof AsmPhyReg) {
                                beColored = preColored.get(save);
                            }
                        }
                        if (FI == 1) {
                            if (save instanceof AsmFVirReg) {
                                beColored = color.get(save);
                            }
                            if (save instanceof AsmFPhyReg) {
                                beColored = preColored.get(save);
                            }
                        }
                        if (FI == 0 && (beColored == 1 || (beColored >= 6 && beColored <= 7) || (beColored >= 11 && beColored <= 11) || (beColored >= 12 && beColored <= 17) || (beColored >= 28 && beColored <= 31))) {
                            int spillPlace = function.getAllocaSize() + function.getArgsSize();
                            if (downOperandMap.containsKey(save) && downOperandMap.get(save).getPointerLevel() == 0) {
                                function.addAllocaSize(4);
                                newAllocSize += 4;
                                storeOrLoadFromMemory(spillPlace, save, instrHead, "store", 0, 0);
                                storeOrLoadFromMemory(spillPlace, save, (AsmInstr) instrHead.getNext(), "load", 1, 0);
                            } else {
                                function.addAllocaSize(8);
                                newAllocSize += 8;
                                storeDOrLoadDFromMemory(spillPlace, save, instrHead, "store", 0, 0);
                                storeDOrLoadDFromMemory(spillPlace, save, (AsmInstr) instrHead.getNext(), "load", 1, 0);
                            }
                        }
                        if (FI == 1 && ((beColored <= 39 && beColored >= 32) || (beColored >= 43 && beColored <= 49) || (beColored >= 60 && beColored <= 63))) {
                            int spillPlace = function.getAllocaSize() + function.getArgsSize();
                            function.addAllocaSize(4);
                            newAllocSize += 4;
                            storeOrLoadFromMemory(spillPlace, save, instrHead, "store", 0, 0);
                            storeOrLoadFromMemory(spillPlace, save, (AsmInstr) instrHead.getNext(), "load", 1, 0);
                        }
                    }
                }
                instrHead = (AsmInstr) instrHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
        return newAllocSize;
    }
    //calleeSave 保存的寄存器x2(sp),x8(s0/fp),x9(s1),x18-27(s2-11)

    private int calleeSave(AsmFunction function) {
        int newAllocSize = 0;
        HashSet<Integer> beChanged = new HashSet<>();
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (instrHead != null) {
                for (AsmReg save : instrHead.regDef) {
                    int beColored = 0;
                    if (FI == 0 && save instanceof AsmVirReg) {
                        beColored = color.get(save);
                    }
                    if (FI == 0 && save instanceof AsmPhyReg) {
                        beColored = preColored.get(save);
                    }
                    if (FI == 1 && save instanceof AsmFVirReg) {
                        beColored = color.get(save);
                    }
                    if (FI == 1 && save instanceof AsmFPhyReg) {
                        beColored = preColored.get(save);
                    }

                    //删除了对sp的保存
                    if (FI == 0 && (beColored == 8 || beColored == 9 || (beColored <= 27 && beColored >= 18))) {
                        beChanged.add(beColored);
                    }
                    if (FI == 1 && (beColored == 40 || beColored == 41 || (beColored <= 59 && beColored >= 50))) {
                        beChanged.add(beColored);
                    }
                }
                instrHead = (AsmInstr) instrHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
        AsmInstr instrHead = (AsmInstr) ((AsmBlock) function.getBlocks().getHead()).getInstrs().getHead();
        AsmInstr instrTail = (AsmInstr) function.getTailBlock().getInstrs().getTail();
        if (FI == 0) {
            for (int save : beChanged) {
                AsmReg sav = RegGeter.AllRegsInt.get(save);
                int spillPlace = function.getAllocaSize() + function.getArgsSize();
//              AsmSw store = new AsmSw(sav, RegGeter.SP, place);
//              AsmLw load = new AsmLw(sav, RegGeter.SP, place);
//              ((AsmBlock) function.getBlocks().getHead()).addInstrHead(store);
//              load.insertBefore(function.getTailBlock().getInstrTail());
                function.addAllocaSize(8);
                newAllocSize += 8;
                AsmInstr firstrHead = (AsmInstr) ((AsmBlock) function.getBlocks().getHead()).getInstrs().getHead();
                storeDOrLoadDFromMemory(spillPlace, sav, firstrHead, "store", 0, 0);
                storeDOrLoadDFromMemory(spillPlace, sav, (AsmInstr) function.getTailBlock().getInstrTail(), "load", 0, 0);
            }
        }
        if (FI == 1) {
            for (int save : beChanged) {
                AsmReg sav = RegGeter.AllRegsFloat.get(save - 32);
                int spillPlace = function.getAllocaSize() + function.getArgsSize();
                if (save != 2) {
                    function.addAllocaSize(4);
                    newAllocSize += 4;
                } else {
                    function.addAllocaSize(8);
                    newAllocSize += 8;
                }
                AsmImm12 place = new AsmImm12(spillPlace);
                if (save != 2) {
                    AsmInstr firstrHead = (AsmInstr) ((AsmBlock) function.getBlocks().getHead()).getInstrs().getHead();
                    storeOrLoadFromMemory(spillPlace, sav, firstrHead, "store", 0, 0);
                    storeOrLoadFromMemory(spillPlace, sav, (AsmInstr) function.getTailBlock().getInstrTail(), "load", 0, 0);
//                    AsmFsw store = new AsmFsw(sav, RegGeter.SP, place);
//                    AsmFlw load = new AsmFlw(sav, RegGeter.SP, place);
//                    ((AsmBlock) function.getBlocks().getHead()).addInstrHead(store);
//                    load.insertBefore(function.getTailBlock().getInstrTail());
                }
            }
        }
        return newAllocSize;
    }

    private void allocRealReg(AsmFunction function) {
        if (FI == 0) {
            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
            while (blockHead != null) {
                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                while (instrHead != null) {
                    for (int i = 0; i < instrHead.regUse.size(); i++) {
                        if (instrHead.regUse.get(i) instanceof AsmVirReg) {
                            int nowColor = color.get(instrHead.regUse.get(i));
                            instrHead.changeUseReg(i, instrHead.regUse.get(i), RegGeter.AllRegsInt.get(nowColor));
                        }
                    }
                    for (int i = 0; i < instrHead.regDef.size(); i++) {
                        if (instrHead.regDef.get(i) instanceof AsmVirReg) {
                            int nowColor = color.get(instrHead.regDef.get(i));
                            instrHead.changeDstReg(i, instrHead.regDef.get(i), RegGeter.AllRegsInt.get(nowColor));
                        }
                    }
                    instrHead = (AsmInstr) instrHead.getNext();
                }
                blockHead = (AsmBlock) blockHead.getNext();
            }
        } else {
            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
            while (blockHead != null) {
                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                while (instrHead != null) {
                    for (int i = 0; i < instrHead.regUse.size(); i++) {
                        if (instrHead.regUse.get(i) instanceof AsmFVirReg) {
                            int nowColor = color.get(instrHead.regUse.get(i));
                            instrHead.changeUseReg(i, instrHead.regUse.get(i), RegGeter.AllRegsFloat.get(nowColor - 32));
                        }
                    }
                    for (int i = 0; i < instrHead.regDef.size(); i++) {
                        if (instrHead.regDef.get(i) instanceof AsmFVirReg) {
                            int nowColor = color.get(instrHead.regDef.get(i));
                            instrHead.changeDstReg(i, instrHead.regDef.get(i), RegGeter.AllRegsFloat.get(nowColor - 32));
                        }
                    }
                    instrHead = (AsmInstr) instrHead.getNext();
                }
                blockHead = (AsmBlock) blockHead.getNext();
            }
        }
    }

    private void initial() {
        S.clear();
        T.clear();
        adjList.clear();
        adjSet.clear();
        degree.clear();
        moveList.clear();
        alias.clear();
        color.clear();
        all.clear();
        simplifyWorklist.clear();
        freezeWorkList.clear();
        spillWorkList.clear();
        spilledNodes.clear();
        coalescedNodes.clear();
        coloredNodes.clear();
        selectStack.clear();
        coalescedMoves.clear();
        constrainedMoves.clear();
        frozenMoves.clear();
        worklistMoves.clear();
        activeMoves.clear();
        loopDepths.clear();
    }

    private void PreColor() {
        preColored.clear();
        preColored.put(RegGeter.ZERO, 0);
        preColored.put(RegGeter.RA, 1);
        preColored.put(RegGeter.SP, 2);
        for (int i = 0; i < 32; i++) {
            preColored.put(RegGeter.AllRegsInt.get(i), i);
        }
        for (int i = 32; i < 64; i++) {
            preColored.put(RegGeter.AllRegsFloat.get(i - 32), i);
        }
    }

    private void LivenessAnalysis(AsmFunction function) {
        GetUseAndDef(function);
        GetBlockLiveInAndOut(function);
        GetLiveInterval(function);
        findLiveAtCall(function);//暂时未用到
    }

    private void GetUseAndDef(AsmFunction function) {
        all.clear();
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            blockHead.getDef().clear();
            blockHead.getUse().clear();
            AsmInstr InsHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (InsHead != null) {
                for (AsmReg one : InsHead.regUse) {
                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
                        if (!blockHead.getDef().contains(one)) {
                            blockHead.getUse().add(one);
                            if (CanBeAddToRun(one)) all.add(one);
                        }
                    }
                }
                for (AsmReg one : InsHead.regDef) {
                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
                        if (!blockHead.getDef().contains(one)) {
                            blockHead.getDef().add(one);
                            if (CanBeAddToRun(one)) all.add(one);
                        }
                    }
                }
                InsHead = (AsmInstr) InsHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void findLoopDepth(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            int depth = blockHead.getDeepth() + 1;
            HashMap<AsmOperand, Integer> defanduse = new HashMap<>();
            AsmInstr InsHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (InsHead != null) {
                for (AsmReg one : InsHead.regUse) {
                    if (CanBeAddToRun(one)) {
                        defanduse.putIfAbsent(one, 0);
                        int cur = defanduse.get(one);
                        cur++;
                        defanduse.put(one, cur);
                    }
                }
                for (AsmReg one : InsHead.regDef) {
                    if (CanBeAddToRun(one)) {
                        defanduse.putIfAbsent(one, 0);
                        int cur = defanduse.get(one);
                        cur++;
                        defanduse.put(one, cur);
                    }
                }
                InsHead = (AsmInstr) InsHead.getNext();
            }
            for (HashMap.Entry<AsmOperand, Integer> entry : defanduse.entrySet()) {
                AsmOperand key = entry.getKey();
                int val = entry.getValue();
                loopDepths.putIfAbsent(key, 0);
                int cur = loopDepths.get(key);
                loopDepths.put(key, cur + 10 * depth * val);

            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void GetBlockLiveInAndOut(AsmFunction function) {
        //初始化block and instr 的LiveIn和LiveOut
        {
            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
            while (blockHead != null) {
                blockHead.LiveIn.clear();
                blockHead.LiveOut.clear();
                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                while (instrHead != null) {
                    instrHead.LiveIn.clear();
                    instrHead.LiveOut.clear();
                    instrHead = (AsmInstr) instrHead.getNext();
                }
                blockHead = (AsmBlock) blockHead.getNext();
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            AsmBlock blockTail = (AsmBlock) function.getBlocks().getTail();
            while (blockTail != null) {
                //LiveOut[B_i] <- Union (LiveIn[s]) where s belongs to succ(B_i) ;
                for (AsmBlock succBlock : blockTail.sucs) {
                    for (AsmReg one : succBlock.LiveIn) {
                        blockTail.LiveOut.add(one);
                    }
                }
                //NewLiveIn <- Union (LiveUse[B_i], (LiveOut[B_i] – Def[B_i]));
                HashSet<AsmReg> NewLiveIn = new HashSet<>();
                NewLiveIn.addAll(blockTail.LiveOut);
                NewLiveIn.removeAll(blockTail.getDef());
                NewLiveIn.addAll(blockTail.getUse());
                if (!NewLiveIn.equals(blockTail.LiveIn)) {
                    changed = true;
                    blockTail.LiveIn = NewLiveIn;
                }
                blockTail = (AsmBlock) blockTail.getPrev();
            }
        }
    }

    private void GetLiveInterval(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrTail = (AsmInstr) blockHead.getInstrs().getTail();
            instrTail.LiveOut.addAll(blockHead.LiveOut);
            while (instrTail != null) {
                if (instrTail.getNext() != null) {
                    instrTail.LiveOut.addAll(((AsmInstr) instrTail.getNext()).LiveIn);
                }
                instrTail.LiveIn.addAll(instrTail.LiveOut);
                instrTail.LiveIn.removeAll(instrTail.regDef);
                instrTail = (AsmInstr) instrTail.getPrev();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void findLiveAtCall(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
            while (instrHead != null) {
                if (instrHead instanceof AsmCall) {
                    S.addAll(instrHead.LiveOut);
                }
                instrHead = (AsmInstr) instrHead.getNext();
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
        T.addAll(all);
        T.removeAll(S);
    }

    private void build(AsmFunction function) {
        //初始化邻接表
        for (AsmOperand one : all) {
            adjList.put(one, new HashSet<>());
            degree.put(one, 0);
        }
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            AsmInstr instrTail = (AsmInstr) blockHead.getInstrs().getTail();
            HashSet<AsmReg> live = new HashSet<>();
            live.addAll(blockHead.LiveOut);
            while (instrTail != null) {
//                if (instrTail instanceof AsmMove  && ((AsmMove) instrTail).getSrc() instanceof AsmReg && ((AsmMove) instrTail).getDst() instanceof  AsmReg) {
//                    AsmMove instrMove = (AsmMove) instrTail;
//                    live.removeAll(instrMove.regUse);
//                    HashSet<AsmOperand> union = new HashSet<>();
//                    //union.addAll(instrTail.regDef);
//                    //union.addAll(instrTail.regUse);
//                    for (AsmOperand D: instrMove.regDef) {
//                        if (CanBeAddToRun(D) || (D instanceof AsmPhyReg && FI == 0) || (D instanceof AsmFPhyReg && FI == 1)) {
//                            union.add(D);
//                        }
//                    }
//                    for (AsmOperand U: instrMove.regUse) {
//                        if (CanBeAddToRun(U) || (U instanceof AsmPhyReg && FI == 0) || (U instanceof AsmFPhyReg && FI == 1)) {
//                            union.add(U);
//                        }
//                    }
//                    if (!union.isEmpty()) {
//                        for (AsmOperand n : union) {
//                            if (!moveList.containsKey(n)) {
//                                moveList.put(n, new HashSet<>());
//                            }
//                            moveList.get(n).add(instrMove);
//                        }
//                        worklistMoves.add(instrMove);
//                    }
//                }

                //live.addAll(instrTail.regDef);
                if (instrTail != null) {
                    if (instrTail instanceof AsmMove && FI == 1) {
                        int i = 0;
                    }
                }
                for (AsmReg D : instrTail.regDef) {
                    if (CanBeAddToRun(D) || (D instanceof AsmPhyReg && FI == 0) || (D instanceof AsmFPhyReg && FI == 1)) {
                        live.add(D);
                    }
                }
                for (AsmReg b : instrTail.regDef) {
                    if (CanBeAddToRun(b) || (b instanceof AsmPhyReg && FI == 0) || (b instanceof AsmFPhyReg && FI == 1)) {
                        for (AsmOperand l : live) {
                            AddEdge(b, l);
                        }
                    }
                }

                //live.addAll(instrTail.regUse);
                live.removeAll(instrTail.regDef);
                for (AsmReg U : instrTail.regUse) {
                    if (CanBeAddToRun(U) || (U instanceof AsmPhyReg && FI == 0) || (U instanceof AsmFPhyReg && FI == 1)) { //不确定是否要要算上预着色的，但应该要算，所以先按算的来/todo
                        live.add(U);
                    }
                }
                //删除无所谓
                instrTail = (AsmInstr) instrTail.getPrev();
                if (instrTail != null) {
                    instrTail.LiveOut.clear();
                    instrTail.LiveOut.addAll(live);
                }
            }
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }

    private void makeWorkList() {
        for (AsmOperand n : all) {
            if (degree.get(n) >= K) {
                spillWorkList.add(n);
            } else if (moveList.containsKey(n) && !moveList.get(n).isEmpty()) {
                freezeWorkList.add(n);
            } else {
                simplifyWorklist.add(n);
            }
        }
    }

    private void simplify() {
        AsmOperand n = simplifyWorklist.iterator().next();
//        AsmOperand n ;
//        if (simplifyWorklist.size() > 20) {
//             n = simplifyWorklist.iterator().next();
//        } else {
//            int small = -1;
//             n = null;
//            for (AsmOperand one: simplifyWorklist) {
//                if (small == -1) {
//                    n = one;
//                    small = loopDepths.get(one);
//                } else {
//                    if ((loopDepths.get(one) * 100 + 5000 ) * (degree.get(n) * 10 + 100) < (loopDepths.get(n) * 100 + 5000) * (degree.get(one) * 10 + 100)) {
//                        n = one;
//                        small = loopDepths.get(one);
//                    }
//                }
//            }
//        }
        simplifyWorklist.remove(n);
        selectStack.push(n);
        for (AsmOperand m : Adjacent(n)) {
            DecrementDegree(m);
        }
    }

    private void Coalesce() {
        AsmInstr m = worklistMoves.iterator().next();
        AsmOperand x = GetAlias(((AsmMove) m).getDst());
        AsmOperand y = GetAlias(((AsmMove) m).getSrc());
        AsmOperand u;
        AsmOperand v;
        if (preColored.containsKey(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(m);
        if (u == v) {
            coalescedMoves.add(m);
            AddWorkList(u);
        } else if (preColored.containsKey(v) || (adjSet.containsKey(u) && adjSet.get(u).contains(v))) {//uv冲突或者uv都是预着色点
            constrainedMoves.add(m);
            AddWorkList(u);
            AddWorkList(v);
        } else {
            boolean Ok = true;
            for (AsmOperand t : Adjacent(v)) {
                if (!OK(t, u)) {
                    Ok = false;
                    break;
                }
            }
            HashSet<AsmOperand> union = new HashSet<>();
            union.addAll(Adjacent(u));
            union.addAll(Adjacent(v));
            if ((preColored.containsKey(u) && Ok) || (!preColored.containsKey(u) && Conservative(union))) {
                coalescedMoves.add(m);
                Combine(u, v);
                AddWorkList(u);
            } else {
                activeMoves.add(m);
            }
        }
    }

    private void Combine(AsmOperand u, AsmOperand v) {
        if (freezeWorkList.contains(v)) {
            freezeWorkList.remove(v);
        } else {
            spillWorkList.remove(v);
        }
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList.get(u).addAll(moveList.get(v));
        HashSet<AsmOperand> V = new HashSet<>();
        V.add(v);
        EnableMoves(V);
        for (AsmOperand t : Adjacent(v)) {
            AddEdge(t, u);
            DecrementDegree(t);
        }
        if ((degree.containsKey(u) && degree.get(u) >= K) && freezeWorkList.contains(u)) {
            freezeWorkList.remove(u);
            spillWorkList.add(u);
        }
    }

    private void Freeze() {
        AsmOperand u = freezeWorkList.iterator().next();
        freezeWorkList.remove(u);
        simplifyWorklist.add(u);

    }

    private void FreezeMoves(AsmOperand u) {
        for (AsmInstr m : NodeMoves(u)) {
            AsmOperand x = ((AsmMove) m).getDst();
            AsmOperand y = ((AsmMove) m).getSrc();
            AsmOperand v;
            if (GetAlias(y) == GetAlias(u)) {
                v = GetAlias(x);
            } else {
                v = GetAlias(y);
            }
            activeMoves.remove(m);
            frozenMoves.add(m);
            if (NodeMoves(v).isEmpty() && (degree.containsKey(v) && degree.get(v) < K)) {
                freezeWorkList.remove(v);
                simplifyWorklist.add(v);
            }
        }
    }

    private void SelectSpill() {
        AsmOperand m = spillWorkList.iterator().next();//目前是随机选，后面再换/todo/
//        int small = -1;
//        AsmOperand m = null;
//        for (AsmOperand one: spillWorkList) {
//            if (small == -1) {
//                m = one;
//                small = loopDepths.get(one);
//            } else {
//                if (loopDepths.get(one) * (degree.get(one) + 1 ) < loopDepths.get(m)  * (degree.get(m) + 1)) {
//                    m = one;
//                    small = loopDepths.get(one);
//                }
//            }
//        }
        spillWorkList.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    private void AssignColors() {
        while (!selectStack.isEmpty()) {
            AsmOperand n = selectStack.pop();
            ArrayList<Integer> okColors = new ArrayList<>(); //换成固定取色
            if (FI == 0) {
                for (int k = 0; k < K; k++) {
                    if (k >= 6 && k != 10 && !( k >= 10&& k <= 17))
                        okColors.add(k);
                }
            } else {
                for (int k = 32; k < K; k++) {
                    if (k != 42)
                        okColors.add(k);
                }
            }
            for (AsmOperand w : adjList.get(n)) {
                AsmOperand Gw = GetAlias(w);
                if (coloredNodes.contains(Gw) || preColored.containsKey(Gw)) {
                    if (coloredNodes.contains(Gw)) {
                        okColors.remove(color.get(Gw));
                    } else {
                        okColors.remove(preColored.get(Gw));
                    }
                }
            }
            if (okColors.isEmpty()) {
                spilledNodes.add(n);
            } else {
                coloredNodes.add(n);
                int c = okColors.iterator().next();
                color.put(n, c);
            }

        }
        for (AsmOperand n : coalescedNodes) {
            if (color.containsKey(GetAlias(n))) {
                color.put(n, color.get(GetAlias(n)));
            } else if (preColored.containsKey(GetAlias(n))) {
                AsmOperand n2 = GetAlias(n);
                int preColor = preColored.get(n2);
                color.put(n, preColor);
            } else {
                int i = 0;
                AsmOperand m = GetAlias(n);
                i = 1;
            }
        }
    }

    private int RewriteProgram(AsmFunction function) {
        int newAllocSize = 0;
        HashSet<AsmOperand> newTemps = new HashSet<>();
        if (FI == 0) {
            for (AsmOperand v : spilledNodes) {
                int spillPlace = function.getAllocaSize() + function.getArgsSize();
                if (downOperandMap.containsKey(v) && downOperandMap.get(v).getPointerLevel() == 0) {
                    function.addAllocaSize(4);
                    newAllocSize += 4;
                } else {
                    function.addAllocaSize(8);
                    newAllocSize += 8;
                }
                AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
                while (blockHead != null) {
                    AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                    while (instrHead != null) {
                        //为regUse进行spill处理
                        if (instrHead.regUse.contains(v)) {
                            AsmVirReg v1 = new AsmVirReg();
                            if (downOperandMap.containsKey(v))
                                downOperandMap.put(v1, downOperandMap.get(v));
                            for (int i = 0; i < instrHead.regUse.size(); i++) {
                                if (instrHead.regUse.get(i) == v) {
                                    instrHead.changeUseReg(i, instrHead.regUse.get(i), v1);
                                }
                            }
                            if (downOperandMap.containsKey(v) && downOperandMap.get(v).getPointerLevel() == 0) {
                                storeOrLoadFromMemory(spillPlace, v1, instrHead, "load", 0, 1);
                            } else {
                                storeDOrLoadDFromMemory(spillPlace, v1, instrHead, "load", 0, 1);
                            }
//                            AsmImm12 place = new AsmImm12(spillPlace);
//                            AsmLw load = new AsmLw(v1, RegGeter.SP, place);
//                            load.insertBefore(instrHead);
                            newTemps.add(v1);
                        }
                        if (instrHead.regDef.contains(v)) {
                            AsmVirReg v2 = new AsmVirReg();
                            if (downOperandMap.containsKey(v))
                                downOperandMap.put(v2, downOperandMap.get(v));
                            for (int i = 0; i < instrHead.regDef.size(); i++) {
                                if (instrHead.regDef.get(i) == v) {
                                    instrHead.changeDstReg(i, instrHead.regDef.get(i), v2);
                                }
                            }
                            if (downOperandMap.containsKey(v) && downOperandMap.get(v).getPointerLevel() == 0) {
                                storeOrLoadFromMemory(spillPlace, v2, instrHead, "store", 1, 1);
                            } else {
                                storeDOrLoadDFromMemory(spillPlace, v2, instrHead, "store", 1, 1);
                            }
//                            AsmImm12 place = new AsmImm12(spillPlace);
//                            AsmSw store = new AsmSw(v2, RegGeter.SP, place);
//                            store.insertAfter(instrHead);
                            newTemps.add(v2);
                        }
                        instrHead = (AsmInstr) instrHead.getNext();

                    }
                    blockHead = (AsmBlock) blockHead.getNext();
                }
            }
        } else {
            for (AsmOperand v : spilledNodes) {
                int spillPlace = function.getAllocaSize() + function.getArgsSize();
                function.addAllocaSize(4);
                newAllocSize += 4;
                AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
                while (blockHead != null) {
                    AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
                    while (instrHead != null) {
                        //为regUse进行spill处理
                        if (instrHead.regUse.contains(v)) {
                            AsmFVirReg v1 = new AsmFVirReg();
                            for (int i = 0; i < instrHead.regUse.size(); i++) {
                                if (instrHead.regUse.get(i) == v) {
                                    instrHead.changeUseReg(i, instrHead.regUse.get(i), v1);
                                }
                            }
                            storeOrLoadFromMemory(spillPlace, v1, instrHead, "load", 0, 1);
//                            AsmImm12 place = new AsmImm12(spillPlace);
//                            AsmFlw load = new AsmFlw(v1, RegGeter.SP, place);
//                            load.insertBefore(instrHead);
                            newTemps.add(v1);
                        }
                        if (instrHead.regDef.contains(v)) {
                            AsmFVirReg v2 = new AsmFVirReg();
                            for (int i = 0; i < instrHead.regDef.size(); i++) {
                                if (instrHead.regDef.get(i) == v) {
                                    instrHead.changeDstReg(i, instrHead.regDef.get(i), v2);
                                }
                            }
                            storeOrLoadFromMemory(spillPlace, v2, instrHead, "store", 1, 1);
//                            AsmImm12 place = new AsmImm12(spillPlace);
//                            AsmFsw store = new AsmFsw(v2, RegGeter.SP, place);
//                            store.insertAfter(instrHead);
                            newTemps.add(v2);
                        }
                        instrHead = (AsmInstr) instrHead.getNext();

                    }
                    blockHead = (AsmBlock) blockHead.getNext();
                }
            }
        }
        return newAllocSize;
    }

    private boolean Conservative(HashSet<AsmOperand> nodes) {
        int k = 0;
        for (AsmOperand n : nodes) {
            if (degree.containsKey(n) && degree.get(n) >= K) {
                k = k + 1;
            }
        }
        return k < K;
    }

    private boolean OK(AsmOperand t, AsmOperand r) {
        if ((degree.containsKey(t) && degree.get(t) < K) || preColored.containsKey(t) || (adjSet.containsKey(t) && adjSet.get(t).contains(r))) {
            return true;
        } else {
            return false;
        }
    }

    private void AddWorkList(AsmOperand u) {
        if (!preColored.containsKey(u) && !MoveRelated(u) && degree.get(u) < K) {
            freezeWorkList.remove(u);
            simplifyWorklist.add(u);
        }
    }

    private AsmOperand GetAlias(AsmOperand n) {//递归寻找被结合的点
        if (coalescedNodes.contains(n)) {
            return GetAlias(alias.get(n));
        }
        return n;
    }

    private void DecrementDegree(AsmOperand m) {
        if (!degree.containsKey(m)) {
            return;
        }
        int d = degree.get(m);
        degree.put(m, d - 1);
        if (d == K - 1) {
            HashSet<AsmOperand> union = new HashSet<>();
            union.addAll(Adjacent(m));
            union.add(m);
            EnableMoves(union);
            spillWorkList.remove(m);
            if (MoveRelated(m)) {
                freezeWorkList.add(m);
            } else {
                simplifyWorklist.add(m);
            }

        }
    }

    private boolean MoveRelated(AsmOperand m) {
        return !NodeMoves(m).isEmpty();
    }

    private void EnableMoves(HashSet<AsmOperand> nodes) {
        for (AsmOperand n : nodes) {
            for (AsmInstr m : NodeMoves(n)) {
                if (activeMoves.contains(m)) {
                    activeMoves.remove(m);
                    worklistMoves.add(m);
                }
            }
        }
    }

    public HashSet<AsmOperand> Adjacent(AsmOperand n) {
        HashSet<AsmOperand> result = new HashSet<>();
        if (adjList.containsKey(n)) {
            result.addAll(adjList.get(n));
            result.removeAll(selectStack);
            result.removeAll(coalescedNodes);
        }
        return result;
    }

    public HashSet<AsmInstr> NodeMoves(AsmOperand n) { //与操作数相关的传送指令集合（未被冻结
        HashSet<AsmInstr> result = new HashSet<>();
        if (!moveList.containsKey(n)) {
            return result;
        }
        result.addAll(moveList.get(n));
        HashSet<AsmInstr> union = new HashSet<>();
        union.addAll(activeMoves);
        union.addAll(worklistMoves);
        result.retainAll(union);
        return result;
    }

    private void AddEdge(AsmOperand b, AsmOperand l) { //未按照书中所给实现
        if (!(adjSet.containsKey(b) && adjSet.get(b).contains(l)) && b != l) {
            if (!adjSet.containsKey(b)) {
                adjSet.put(b, new HashSet<>());
            }
            if (!adjSet.containsKey(l)) {
                adjSet.put(l, new HashSet<>());
            }
            adjSet.get(b).add(l);
            adjSet.get(l).add(b);
            if (!preColored.containsKey(b) && (l != RegGeter.AllRegsInt.get(5)) && (l != RegGeter.AllRegsInt.get(10))) {
                adjList.get(b).add(l);
                degree.put(b, degree.get(b) + 1);
            }
            if (!preColored.containsKey(l) && (b != RegGeter.AllRegsInt.get(5)) && (b != RegGeter.AllRegsInt.get(10))) {
                adjList.get(l).add(b);
                degree.put(l, degree.get(l) + 1);
            }
        }
    }

    private boolean CanBeAddToRun(AsmOperand m) {
        if ((m instanceof AsmVirReg && FI == 0) || (m instanceof AsmFVirReg && FI == 1)) {
            return true;
        }
//       else if ((m instanceof AsmPhyReg && FI == 0) || (m instanceof AsmFPhyReg && FI == 1)) {
//           return true;
//       }
        else {
            return false;
        }
    }

    private static boolean debug = false;

    private void logout(String s) {
        if (debug) {
            System.out.println(s);
        }
    }
}
