package frontend.ir.instr.binop;

import frontend.ir.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class FDivInstr extends BinaryOperation {
    public FDivInstr(int result, Value op1, Value op2, BasicBlock parentBB) {
        super(result, op1, op2, "fdiv", DataType.FLOAT, parentBB);
        assert op1.getDataType() == DataType.FLOAT;
        assert op2.getDataType() == DataType.FLOAT;
    }
}
