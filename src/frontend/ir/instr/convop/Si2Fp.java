package frontend.ir.instr.convop;

import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;

public class Si2Fp extends ConversionOperation{
    public Si2Fp(int result, Value value) {
        super(result, DataType.INT, DataType.FLOAT, value, "sitofp");
    }
    
    @Override
    public Instruction cloneShell(Function parentFunc) {
        return new Si2Fp(parentFunc.getAndAddRegIndex(), this.value);
    }
    
    @Override
    public Value operationSimplify() {
        if (this.value instanceof ConstInt) {
            return new ConstFloat(value.getNumber().floatValue());
        }
        return null;
    }
}
