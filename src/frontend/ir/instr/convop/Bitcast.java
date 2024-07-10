package frontend.ir.instr.convop;

import frontend.ir.DataType;
import frontend.ir.Value;

/**
 * 用于将整数、浮点数的（数组）指针转化为 void*，即 i8，用于 memset
 */
public class Bitcast extends ConversionOperation {
    public Bitcast(int result, Value value) {
        super(result, value.getDataType(), DataType.VOID_, value, "bitcast");
    }
}
