package backend;

import backend.asmInstr.AsmInstr;
import backend.asmInstr.asmTermin.AsmCall;
import backend.itemStructure.AsmBlock;
import backend.itemStructure.AsmFunction;
import backend.itemStructure.AsmModule;
import backend.itemStructure.AsmOperand;
import backend.regs.*;
import frontend.ir.Value;

import java.util.*;

public class RegAllocLinear {
    private RegAllocLinear(HashMap<AsmOperand, Value> downOperandMap) {
        // 初始化逻辑
        this.downOperandMap = downOperandMap;
    }
    private HashMap<AsmOperand, Value> downOperandMap;
    private static RegAllocLinear instance = null;
    private HashSet<AsmOperand> all = new HashSet<>();
    private HashMap<AsmOperand, Interval> intervals = new HashMap<>();
    private List<Interval> sortedIntervals = new ArrayList<>();
    private Stack<Integer> freeRegisters = new Stack<>();
    // 提供一个公共的静态方法，用于获取单例对象
    public static synchronized RegAllocLinear getInstance(HashMap<AsmOperand, Value> downOperandMap) {
        if (instance == null) {
            instance = new RegAllocLinear(downOperandMap);
        }
        return instance;
    }
//    private int FI = 0;
//    HashMap<AsmOperand, Integer> loopDepths = new HashMap<>();
//    private void LivenessAnalysis(AsmFunction function) {
//        GetUseAndDef(function);
//        GetBlockLiveInAndOut(function);
//        GetLiveInterval(function);
//    }
//    private void GetUseAndDef(AsmFunction function) {
//        all.clear();
//        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
//        while (blockHead != null) {
//            blockHead.getDef().clear();
//            blockHead.getUse().clear();
//            AsmInstr InsHead = (AsmInstr) blockHead.getInstrs().getHead();
//            while (InsHead != null) {
//                for (AsmReg one : InsHead.regUse) {
//                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
//                        if (!blockHead.getDef().contains(one)) {
//                            blockHead.getUse().add(one);
//                            if (CanBeAddToRun(one)) all.add(one);
//                        }
//                    }
//                }
//                for (AsmReg one : InsHead.regDef) {
//                    if (CanBeAddToRun(one) || (one instanceof AsmPhyReg && FI == 0) || (one instanceof AsmFPhyReg && FI == 1)) {
//                        if (!blockHead.getDef().contains(one)) {
//                            blockHead.getDef().add(one);
//                            if (CanBeAddToRun(one)) all.add(one);
//                        }
//                    }
//                }
//                InsHead = (AsmInstr) InsHead.getNext();
//            }
//            blockHead = (AsmBlock) blockHead.getNext();
//        }
//    }
//    private void GetBlockLiveInAndOut(AsmFunction function) {
//        //初始化block and instr 的LiveIn和LiveOut
//        {
//            AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
//            while (blockHead != null) {
//                blockHead.LiveIn.clear();
//                blockHead.LiveOut.clear();
//                AsmInstr instrHead = (AsmInstr) blockHead.getInstrs().getHead();
//                while (instrHead != null) {
//                    instrHead.LiveIn.clear();
//                    instrHead.LiveOut.clear();
//                    instrHead = (AsmInstr) instrHead.getNext();
//                }
//                blockHead = (AsmBlock) blockHead.getNext();
//            }
//        }
//        boolean changed = true;
//        while (changed) {
//            changed = false;
//            AsmBlock blockTail = (AsmBlock) function.getBlocks().getTail();
//            while (blockTail != null) {
//                //LiveOut[B_i] <- Union (LiveIn[s]) where s belongs to succ(B_i) ;
//                for (AsmBlock succBlock : blockTail.sucs) {
//                    for (AsmReg one : succBlock.LiveIn) {
//                        blockTail.LiveOut.add(one);
//                    }
//                }
//                //NewLiveIn <- Union (LiveUse[B_i], (LiveOut[B_i] – Def[B_i]));
//                HashSet<AsmReg> NewLiveIn = new HashSet<>();
//                NewLiveIn.addAll(blockTail.getUse());
//                NewLiveIn.addAll(blockTail.LiveOut);
//                NewLiveIn.removeAll(blockTail.getDef());
//
//                if (!NewLiveIn.equals(blockTail.LiveIn)) {
//                    changed = true;
//                    blockTail.LiveIn = NewLiveIn;
//                }
//                blockTail = (AsmBlock) blockTail.getPrev();
//            }
//        }
//    }
//    private void GetLiveInterval(AsmFunction function) {
//        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
//        while (blockHead != null) {
//            AsmInstr instrTail = (AsmInstr) blockHead.getInstrs().getTail();
//            HashSet<AsmReg> live = new HashSet<>();
//            live.addAll(blockHead.LiveOut);
//            while (instrTail != null) {
//                for (AsmReg D : instrTail.regDef) {
//                    if (CanBeAddToRun(D) || (D instanceof AsmPhyReg && FI == 0) || (D instanceof AsmFPhyReg && FI == 1)) {
//                        live.add(D);
//                    }
//                }
//                live.removeAll(instrTail.regDef);
//                for (AsmReg U : instrTail.regUse) {
//                    if (CanBeAddToRun(U) || (U instanceof AsmPhyReg && FI == 0) || (U instanceof AsmFPhyReg && FI == 1)) { //不确定是否要要算上预着色的，但应该要算，所以先按算的来/todo
//                        live.add(U);
//                    }
//                }
//                instrTail.LiveIn.clear();
//                instrTail.LiveIn.addAll(live);
//                instrTail = (AsmInstr) instrTail.getPrev();
//                if (instrTail != null) {
//                    instrTail.LiveOut.clear();
//                    instrTail.LiveOut.addAll(live);
//                }
//            }
//            blockHead = (AsmBlock) blockHead.getNext();
//        }
//    }
//    private boolean CanBeAddToRun(AsmOperand m) {
//        if ((m instanceof AsmVirReg && FI == 0) || (m instanceof AsmFVirReg && FI == 1)) {
//            return true;
//        }
////       else if ((m instanceof AsmPhyReg && FI == 0) || (m instanceof AsmFPhyReg && FI == 1)) {
////           return true;
////       }
//        else {
//            return false;
//        }
//    }
//    // 新增的代码部分

//    private void LinearScanRegisterAllocation(AsmFunction function) {
//        // 初始化空闲寄存器池
//        for (int i = 6; i < 32; i++) {// 假设我们有个寄存器
//            if (i != 10)
//            freeRegisters.push(i);
//        }
//
//        // 按照起始点对活跃区间进行排序
//        sortedIntervals.sort(Comparator.comparingInt(Interval::getStart));
//
//        // 线性扫描
//        List<Interval> active = new ArrayList<>();
//        for (Interval interval : sortedIntervals) {
//            ExpireOldIntervals(active, interval);
//            if (active.size() == 26) { // 假设我们有32个寄存器
//                SpillAtInterval(active, interval);
//            } else {
//                interval.setColor(freeRegisters.pop());
//                active.add(interval);
//                active.sort(Comparator.comparingInt(Interval::getEnd));
//            }
//        }
//    }

//    private void ExpireOldIntervals(List<Interval> active, Interval current) {
//        Iterator<Interval> iterator = active.iterator();
//        while (iterator.hasNext()) {
//            Interval interval = iterator.next();
//            if (interval.getEnd() >= current.getStart()) {
//                return;
//            }
//            iterator.remove();
//            freeRegisters.push(interval.getColor());
//        }
//    }

//    private void SpillAtInterval(List<Interval> active, Interval current) {
//        // Find the interval with the latest end time that is not a physical register
//        Interval spill = null;
//        for (Interval interval : active) {
//            if (!(interval.getRegister() instanceof AsmPhyReg)) {
//                if (spill == null || interval.getEnd() > spill.getEnd()) {
//                    spill = interval;
//                }
//            }
//        }
//
//        // If no suitable interval to spill, spill the current interval
//        if (spill == null) {
//            current.setSpilled(true);
//            return;
//        }
//
//        // Spill the selected interval
//        if (spill.getEnd() > current.getEnd()) {
//            current.setColor(spill.getColor());
//            spill.setSpilled(true);
//            active.remove(spill);
//            active.add(current);
//            active.sort(Comparator.comparingInt(Interval::getEnd));
//        } else {
//            current.setSpilled(true);
//        }
//    }

    // 你需要实现的Interval类
    private class Interval {
        private AsmOperand register;
        private int color = -1;
        public ArrayList<Range> RangeList = new ArrayList<>();
        public ArrayList<UsePostion> UsePostionList = new ArrayList<>();
        public Interval(AsmOperand register) {
            this.register = register;
        }
        public void addRange(int from, int to) {
            Range newRange = new Range(from, to);
            int insertPos = 0;
            // 查找插入位置，保持有序
            for (int i = 0; i < RangeList.size(); i++) {
                Range range = RangeList.get(i);
                if (range.from > from) {
                    insertPos = i;
                    break;
                } else if (i == RangeList.size() - 1) {
                    insertPos = RangeList.size();
                }
            }
            // 插入新的区间
            RangeList.add(insertPos, newRange);
            // 合并区间
            mergeRanges();
        }
        private void mergeRanges() {
            ArrayList<Range> mergedRanges = new ArrayList<>();
            Range current = null;

            for (Range range : RangeList) {
                if (current == null) {
                    current = range;
                } else {
                    if (current.to >= range.from) {
                        current.to = Math.max(current.to, range.to);
                    } else {
                        mergedRanges.add(current);
                        current = range;
                    }
                }
            }

            if (current != null) {
                mergedRanges.add(current);
            }

            RangeList = mergedRanges;
        }

        public void add_use_pos(int pos, int kind) {
            UsePostionList.add(new UsePostion(pos,kind));
        }

    }
    private class Range {
        public int from;
        public int to;
        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    private class UsePostion {
        public int position;
        public int use_kind;
        public UsePostion(int position, int use_kind) {
            this.position = position;
            this.use_kind = use_kind;
        }
    }



    ///////
//    // 将方法的第一个基本块添加到工作列表
//    append first block of method to work_list
//
//// 当工作列表不为空时，进行以下操作
//while work_list is not empty do
//    // 从工作列表中选取并移除第一个基本块
//    BlockBegin b = pick and remove first block from work_list
//    // 将选取的基本块添加到blocks集合
//    append b to blocks
//    // 遍历基本块b的所有后继基本块
//    for each successor sux of b do
//    // 减少后继基本块的前向分支计数
//    decrement sux.incoming_forward_branches
//    // 如果后继基本块的前向分支计数为0
//        if sux.incoming_forward_branches = 0 then
//    // 将后继基本块按顺序插入工作列表
//    sort sux into work_list
//    end if
//    end for
//    end while
    ArrayList<AsmBlock> blockWorkList = new ArrayList<>();
    ArrayList<AsmBlock> blocks = new ArrayList<>();
    HashMap<AsmBlock, Integer> blockPreNum = new HashMap<>();
    private void initial(AsmFunction function) {
        AsmBlock blockHead = (AsmBlock) function.getBlocks().getHead();
        while (blockHead != null) {
            blockPreNum.put(blockHead, blockHead.pres.size());
            blockHead = (AsmBlock) blockHead.getNext();
        }
    }
    private void COMPUTE_BLOCK_ORDER(AsmFunction function) {
        this.blockWorkList.clear();
        this.blocks.clear();
        initial(function);
        blockWorkList.add((AsmBlock) function.getBlocks().getHead());
        while (blockWorkList.size() > 0) {
            AsmBlock block = (AsmBlock) blockWorkList.remove(0);
            blocks.add(block);
            for (AsmBlock successor: block.sucs) {
                blockPreNum.put(successor, blockPreNum.get(successor) - 1);
                if (blockPreNum.get(successor) == 0) {
                    blockWorkList.add(successor);
                }
            }
        }
    }

//    NUMBER_OPERATIONS
//    // 初始化下一个ID为0
//    int next_id = 0
//    // 遍历所有基本块
//    for each block b in blocks do
//            // 遍历基本块b中的所有操作
//            for each operation op in b.operations do
//             // 为操作op分配当前的ID
//              op.id = next_id
//            // 更新下一个ID，增加2
//              next_id = next_id + 2
//            end for
//    end for
    HashMap<AsmInstr, Integer> instrsId = new HashMap<>();
    private void NUMBER_OPERATIONS() {
        int next_id = 0;
        for (AsmBlock block: blocks) {
            AsmInstr instrHead = (AsmInstr) block.getInstrs().getHead();
            while (instrHead != null) {
                instrsId.put(instrHead,next_id);
                next_id+=2;
                instrHead = (AsmInstr) instrHead.getNext();
            }
        }
    }
//    {
//        // 声明一个LIR_OpVisitState类型的访问器，用于收集操作的所有操作数
//        LIR_OpVisitState visitor;
//// 遍历所有基本块
//        for each block b in blocks do
//        // 初始化当前基本块的局部生成集和局部消除集为空集合
//        b.live_gen = { }
//        b.live_kill = { }
//        // 遍历基本块b中的所有操作
//        for each operation op in b.operations do
//        // 使用访问器访问当前操作
//        visitor.visit(op)
//        // 遍历当前操作的输入操作数
//        for each virtual register opr in visitor.input_oprs do
//        // 如果输入操作数不在当前基本块的消除集中，则将其添加到生成集中
//        if opr ∉ b.live_kill then
//        b.live_gen = b.live_gen ∪ { opr }
//        end if
//        end for
//        // 遍历当前操作的临时操作数
//        for each virtual register opr in visitor.temp_oprs do //我们没有临时操作数
//        // 将临时操作数添加到当前基本块的消除集中
//        b.live_kill = b.live_kill ∪ { opr }
//        end for
//        // 遍历当前操作的输出操作数
//        for each virtual register opr in visitor.output_oprs do
//        // 将输出操作数添加到当前基本块的消除集中
//        b.live_kill = b.live_kill ∪ { opr }
//        end for
//        end for
//        end for
//    }
    private void COMPUTE_LOCAL_LIVE_SETS() {
        for (AsmBlock block: blocks) {
            AsmInstr InsHead = (AsmInstr) block.getInstrs().getHead();
            while (InsHead != null) {
                for (AsmReg one : InsHead.regUse) {
                        if (!block.getDef().contains(one)) {
                            block.getUse().add(one);
                             all.add(one);
                        }
                }
                for (AsmReg one : InsHead.regDef) {
                        if (!block.getDef().contains(one)) {
                            block.getDef().add(one);
                             all.add(one);
                        }
                }
                InsHead = (AsmInstr) InsHead.getNext();
            }
        }
    }
//    {
//        COMPUTE_GLOBAL_LIVE_SETS
//        do
//            for each block b in blocks in reverse order do // 反向遍历所有基本块
//        b.live_out = { } // 初始化当前基本块的 live_out 集合为空
//        for each successor sux of b do // 遍历当前基本块的每个后继块
//        b.live_out = b.live_out ∪ sux.live_in // 将后继块的 live_in 集合并入当前块的 live_out 集合
//        end for
//        b.live_in = (b.live_out – b.live_kill) ∪ b.live_gen // 计算当前块的 live_in 集合
//        end for
//        while change occurred in any live set // 只要任何 live 集合发生变化，就继续循环
//    }
    private void COMPUTE_GLOBAL_LIVE_SETS(AsmFunction function) {
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
            for (int i = blocks.size() - 1; i >= 0; i--) {
                AsmBlock blockTail = blocks.get(i);
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
            }
        }
    }
    {
//        BUILD_INTERVALS
//        LIR_OpVisitState visitor; // 访问状态，用于收集操作的所有操作数
//        for each block b in blocks in reverse order do // 反向遍历所有基本块
//        int block_from = b.first_op.id // 获取当前块的起始操作ID
//        int block_to = b.last_op.id + 2 // 获取当前块的结束操作ID并加2
//        for each operand opr in b.live_out do // 遍历当前块的所有活跃操作数
//        intervals[opr].add_range(block_from, block_to) // 为操作数添加区间
//        end for
//        for each operation op in b.operations in reverse order do // 反向遍历当前块的所有操作
//        visitor.visit(op) // 访问操作，收集操作数
//        if visitor.has_call then // 如果操作是一个调用
//        for each physical register reg do // 遍历所有物理寄存器
//        intervals[reg].add_range(op.id, op.id + 1) // 为物理寄存器添加一个长度为1的短区间
//        end for
//        end if
//        for each virtual or physical register opr in visitor.output_oprs do // 遍历所有输出操作数
//        intervals[opr].first_range.from = op.id // 设置区间的起始位置
//        intervals[opr].add_use_pos(op.id, use_kind_for(op, opr)) // 添加使用位置
//        end for
//        for each virtual or physical register opr in visitor.temp_oprs do // 遍历所有临时操作数
//        intervals[opr].add_range(op.id, op.id + 1) // 为临时操作数添加一个长度为1的短区间
//        intervals[opr].add_use_pos(op.id, use_kind_for(op, opr)) // 添加使用位置
//        end for
//        for each virtual or physical register opr in visitor.input_oprs do // 遍历所有输入操作数
//        intervals[opr].add_range(block_from, op.id) // 为输入操作数添加区间
//        intervals[opr].add_use_pos(op.id, use_kind_for(op, opr)) // 添加使用位置 /todo use_kind_for啥意思
//        end for
//        end for
//        end for
    }
    private HashMap<AsmReg, Interval> IntIntervals = new HashMap<>();
    private HashMap<AsmReg, Interval> FloatIntervals = new HashMap<>();
    private  void  BUILD_INTERVALS() {
        for (int b = blocks.size() - 1; b >= 0; b--) {
            AsmBlock blockTail = blocks.get(b);
            int block_from = instrsId.get((AsmInstr) blockTail.getInstrs().getHead());
            int block_to = instrsId.get((AsmInstr) blockTail.getInstrTail()) + 2;
            for (AsmOperand opr: blockTail.LiveOut) {
                if ((opr instanceof AsmPhyReg ) || opr instanceof AsmVirReg) {
                    IntIntervals.putIfAbsent((AsmReg) opr, new Interval(opr));
                    IntIntervals.get(opr).addRange(block_from, block_to);
                }
                if (opr instanceof AsmFPhyReg || opr instanceof AsmFVirReg) {
                    FloatIntervals.putIfAbsent((AsmReg) opr, new Interval(opr));
                    FloatIntervals.get(opr).addRange(block_from, block_to);
                }
            }
            AsmInstr instrTail = (AsmInstr) blockTail.getInstrTail();
            while (instrTail != null) {
                if (instrTail instanceof AsmCall) {
                    for (int i = 0; i < 32; i++) {
                        if (i == 1 || (i >= 5 && i <= 7) || (i >= 11 && i <= 11) || (i >= 12 && i <= 17) || (i >= 28 && i <= 31)) {
                            IntIntervals.putIfAbsent(RegGeter.AllRegsInt.get(i), new Interval(RegGeter.AllRegsInt.get(i)));
                            IntIntervals.get(RegGeter.AllRegsInt.get(i)).addRange(instrsId.get(instrTail), instrsId.get(instrTail) + 1);
                        }
                    }
                    for (int i = 32; i < 64; i++) {
                        if ((i <= 39 && i >= 32) || (i >= 42 && i <= 49) || (i >= 60 && i <= 63)) {
                            FloatIntervals.putIfAbsent(RegGeter.AllRegsFloat.get(i - 32), new Interval(RegGeter.AllRegsFloat.get(i - 32)));
                            FloatIntervals.get(RegGeter.AllRegsFloat.get(i-32)).addRange(instrsId.get(instrTail), instrsId.get(instrTail) + 1);
                        }
                    }
                }
                for (AsmOperand opr: instrTail.regDef) {
                    if (opr instanceof AsmPhyReg || opr instanceof AsmVirReg) {
                        IntIntervals.putIfAbsent((AsmReg) opr, new Interval(opr));
                        if (IntIntervals.get(opr).RangeList.isEmpty()) {
                            IntIntervals.get(opr).addRange(instrsId.get(instrTail), instrsId.get(instrTail) + 1); //todo 不确定
                        }
                        IntIntervals.get(opr).RangeList.get(0).from = instrsId.get(instrTail);
                        IntIntervals.get(opr).add_use_pos(instrsId.get(instrTail), 1);
                    }
                    if (opr instanceof AsmFPhyReg || opr instanceof AsmFVirReg) {
                        FloatIntervals.putIfAbsent((AsmReg) opr, new Interval(opr));
                        FloatIntervals.get(opr).RangeList.get(0).from = instrsId.get(instrTail);
                        FloatIntervals.get(opr).add_use_pos(instrsId.get(instrTail), 1);
                    }
                }
                for (AsmOperand opr: instrTail.regUse) {
                    if (opr instanceof AsmPhyReg || opr instanceof AsmVirReg) {
                        IntIntervals.putIfAbsent((AsmReg) opr, new Interval(opr));
                        IntIntervals.get(opr).addRange(block_from, instrsId.get(instrTail));
                        IntIntervals.get(opr).add_use_pos(instrsId.get(instrTail), 1);
                    }
                }
                instrTail = (AsmInstr) instrTail.getPrev();
            }
        }
    }
//    {
//        WALK_INTERVALS
//                unhandled = list of intervals sorted by increasing start point // 未处理区间列表，按起始点递增排序
//            active = { } // 活动区间集合
//        inactive = { } // 非活动区间集合
//
//// 注意：在分配期间，当区间被分割时，新的区间可能会被排序到unhandled列表中
//        while unhandled ≠ { } do // 当未处理区间不为空时，继续执行
//        current = pick and remove first interval from unhandled // 选取并移除unhandled列表中的第一个区间
//        position = current.first_range.from // 获取当前区间的起始位置
//
//        // 检查活动区间中的区间是否过期或变为非活动
//        for each interval it in active do
//        if it.last_range.to < position then // 如果区间的结束位置小于当前起始位置
//        move it from active to handled // 将区间从active移动到handled（已处理区间集合）
//        else if not it.covers(position) then // 否则，如果区间不覆盖当前起始位置
//        move it from active to inactive // 将区间从active移动到inactive
//        end if
//        end for
//
//        // 检查非活动区间中的区间是否过期或变为活动
//        for each interval it in inactive do
//        if it.last_range.to < position then // 如果区间的结束位置小于当前起始位置
//        move it from inactive to handled // 将区间从inactive移动到handled
//        else if it.covers(position) then // 否则，如果区间覆盖当前起始位置
//        move it from inactive to active // 将区间从inactive移动到active
//        end if
//        end for
//
//        // 为当前区间分配寄存器
//        TRY_ALLOCATE_FREE_REG // 尝试分配空闲寄存器
//        if allocation failed then // 如果分配失败
//            ALLOCATE_BLOCKED_REG // 分配被阻塞的寄存器
//        end if
//
//        if current has a register assigned then // 如果当前区间被分配了寄存器
//        add current to active // 将当前区间添加到active
//        end if
//        end while
//    }
    ArrayList<Interval> unhandled = new ArrayList<>();
    ArrayList<Interval> active = new ArrayList<>();
    ArrayList<Interval> inactive = new ArrayList<>();
    private void initial_( ArrayList<Interval> args) {
        unhandled.clear();
        active.clear();
        inactive.clear();

    }
    private void WALK_INTERVALS() {

    }
    public void debug(AsmModule module) {
        for (AsmFunction function : module.getFunctions()) {
            COMPUTE_BLOCK_ORDER(function);
            NUMBER_OPERATIONS();
            COMPUTE_LOCAL_LIVE_SETS();
            COMPUTE_GLOBAL_LIVE_SETS(function);
            BUILD_INTERVALS();

        }
    }

}

