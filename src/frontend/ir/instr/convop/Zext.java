package frontend.ir.instr.convop;

import frontend.ir.DataType;
import frontend.ir.Value;

/**
 * 目前认为这个指令就是用来从 i1 拓展到 i32 的。
 */
public class Zext extends ConversionOperation {
    public Zext(int result, Value value) {
        super(result, DataType.BOOL, DataType.INT, value, "zext");
    }
}
