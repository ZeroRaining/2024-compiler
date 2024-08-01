package midend.SSA;

import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.AllocaInstr;
import frontend.ir.instr.memop.LoadInstr;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.structure.BasicBlock;
import frontend.ir.structure.Function;
import frontend.ir.structure.Procedure;
import frontend.ir.symbols.Symbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 全局对象简化，包括将没有被更新过的全局对象直接用初值替代，以及【局部化】
 * 安排在函数内联之后、Mem2Reg 之前
 *
 * 全局对象局部化：
 * 现阶段想法是若一个全局变量只被 main 函数使用过，则将其变为局部变量。
 */
public class GlobalValueSimplify {
    public static void execute(List<Symbol> globalSymbols) {
        for (Symbol symbol : globalSymbols) {
            if (!symbol.isGlobal()) {
                throw new RuntimeException("全局变量不是全局的？");
            }
            if (symbol.isArray()) {
                continue;   // todo 现阶段对于数组束手无策
            }
            HashSet<Value> users = symbol.getAllocValue().getUserSet();
            boolean onlyMain = true;
            boolean neverStored = true;
            for (Value user : users) {
                if (!(user instanceof Instruction)) {
                    throw new RuntimeException("使用全局变量地址的应该只有指令");
                }
                
                if (user instanceof StoreInstr) {
                    neverStored = false;
                }
                
                Function func = getFunction((Instruction) user);
                if (!func.isMain()) {
                    onlyMain = false;
                    break;
                }
            }
            
            if (neverStored) {      // 赋初值之后再也没有更新过
                replaceWithInit(symbol, users);
            } else if (onlyMain) {  // 只在 main 里被使用过
                localize(symbol);
            }
        }
    }
    
    private static void replaceWithInit(Symbol symbol, HashSet<Value> users) {
        for (Value user : users) {
            if (user instanceof LoadInstr) {
                user.replaceUseTo(symbol.getInitVal());
                symbol.abandon();
            } else {
                throw new RuntimeException("还没想好除了load之外还有什么可能用到没修改过的全局变量");
            }
        }
    }
    
    private static Function getFunction(Instruction ins) {
        Object blk = ins.getParent().getOwner();
        if (!(blk instanceof BasicBlock)) {
            throw new RuntimeException("指令列表的所有者只能是基本块对象");
        }
        Object pro = ((BasicBlock) blk).getParent().getOwner();
        if (!(pro instanceof Procedure)) {
            throw new RuntimeException("基本块列表的所有者只能是过程对象");
        }
        return ((Procedure) pro).getParentFunc();
    }
    
    private static void localize(Symbol symbol) {
        Symbol localizedSym = new Symbol("@L_" + symbol.getName(), symbol.getType(), new ArrayList<>(),
                symbol.isConstant(), false, null);
        AllocaInstr allocaInstr = new AllocaInstr(Function.MAIN.getAndAddRegIndex(), localizedSym);
        localizedSym.setAllocValue(allocaInstr);
        StoreInstr storeInstr = new StoreInstr(symbol.getInitVal(), localizedSym);
        
        BasicBlock mainBeginBlk = (BasicBlock) Function.MAIN.getBasicBlocks().getHead();
        mainBeginBlk.addInstrToHead(allocaInstr);
        mainBeginBlk.addInstrToHead(storeInstr);
        
        symbol.getAllocValue().replaceUseTo(allocaInstr);
        symbol.abandon();
    }
}
