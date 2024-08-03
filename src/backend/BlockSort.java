package backend;

import backend.asmInstr.asmBr.AsmJ;
import backend.itemStructure.*;
import frontend.ir.Value;
import frontend.syntax.Ast;

import java.util.HashMap;

public class BlockSort {
    private BlockSort() {
        // 初始化逻辑
    }

    // 单例模式下的私有静态成员变量
    private static BlockSort instance = null;

    // 提供一个公共的静态方法，用于获取单例对象
    public static BlockSort getInstance() {
        if (instance == null) {
            instance = new BlockSort();
        }
        return instance;
    }
    public void run(AsmModule module) {

    }
    HashMap<Group, Double> BlocksEdge = new HashMap<>();
    private void initial() {
        BlocksEdge.clear();
    }
    private void CollectionInfo(AsmFunction function) {

    }
    private void selectMergeBlock(AsmFunction function) {
        boolean changed = true;
        while (changed) {
            changed = false;
            AsmBlock i = (AsmBlock) function.getBlocks().getHead();
            while (i != null) {
                if (i.sucs.size() == 1) {
                    AsmBlock j = i.sucs.iterator().next();
                    if (j.pres.size() == 1) {
                        AsmBlock k = j.pres.iterator().next();
                        if (k == i) {
                            mergeBlock(i, j);
                            changed = true;
                        }
                    }
                }
                i = (AsmBlock) i.getNext();
            }
        }
    }
    private void mergeBlock(AsmBlock i, AsmBlock j) {//把j合进i
        if (i.getInstrTail() instanceof AsmJ) {
            i.getInstrTail().removeFromList();
        }
        i.getInstrs().addCustomListToTail(j.getInstrs());
        j.removeFromList();
    }
}
