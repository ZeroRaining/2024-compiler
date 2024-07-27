package frontend.ir.instr.convop;

import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;

public class Fp2Si extends ConversionOperation {
    public Fp2Si(int result, Value value) {
        super(result, DataType.FLOAT, DataType.INT, value, "fptosi");
    }
    
    @Override
    public Instruction cloneShell(Function parentFunc) {
        return new Fp2Si(parentFunc.getAndAddRegIndex(), this.value);
    }
    
    @Override
    public Value operationSimplify() {
        if (this.value instanceof ConstFloat) {
            return new ConstInt(value.getNumber().intValue());
        }
        return null;
    }
}
