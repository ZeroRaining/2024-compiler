package frontend.ir.instr.convop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;

/**
 * 目前认为这个指令就是用来从 i1 拓展到 i32 的。
 */
public class Zext extends ConversionOperation {
    public Zext(int result, Value value, BasicBlock parentBB) {
        super(result, DataType.BOOL, DataType.INT, value, "zext", parentBB);
    }
}
