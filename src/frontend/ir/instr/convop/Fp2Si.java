package frontend.ir.instr.convop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class Fp2Si extends ConversionOperation {
    public Fp2Si(int result, Value value) {
        super(result, DataType.FLOAT, DataType.INT, value, "fptosi");
    }
}
