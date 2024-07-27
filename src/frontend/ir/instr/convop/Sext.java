package frontend.ir.instr.convop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;
import frontend.ir.structure.Procedure;

/**
 * 目前这个指令就是用来 i32 -> i64 的，也就是用于普通整数转成偏移量的操作
 */
public class Sext extends ConversionOperation{
    public Sext(int result, Value value) {
        super(result, DataType.INT, DataType.LONG_INT, value, "sext");
    }
    
    @Override
    public Instruction cloneShell(Procedure procedure) {
        return new Sext(procedure.getAndAddRegIndex(), this.value);
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
}
