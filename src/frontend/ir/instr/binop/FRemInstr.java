package frontend.ir.instr.binop;

import frontend.ir.DataType;
import frontend.ir.Value;

public class FRemInstr extends BinaryOperation {
    public FRemInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "frem", DataType.FLOAT);
        assert op1.getDataType() == DataType.FLOAT;
        assert op2.getDataType() == DataType.FLOAT;
    }
}
