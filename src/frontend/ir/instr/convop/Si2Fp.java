package frontend.ir.instr.convop;

import frontend.ir.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class Si2Fp extends ConversionOperation{
    public Si2Fp(int result, Value value, BasicBlock parentBB) {
        super(result, DataType.INT, DataType.FLOAT, value, "sitofp", parentBB);
    }
}
