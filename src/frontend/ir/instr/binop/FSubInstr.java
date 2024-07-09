package frontend.ir.instr.binop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class FSubInstr extends BinaryOperation {
    public FSubInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "fsub", DataType.FLOAT);
        assert op1.getDataType() == DataType.FLOAT;
        assert op2.getDataType() == DataType.FLOAT;
    }
}
