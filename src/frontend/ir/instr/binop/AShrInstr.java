package frontend.ir.instr.binop;

import frontend.ir.DataType;
import frontend.ir.Value;

public class AShrInstr extends BinaryOperation {
    public AShrInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "ashr", DataType.INT);
        assert op1.getDataType() == DataType.INT;
        assert op2.getDataType() == DataType.INT;
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
}
