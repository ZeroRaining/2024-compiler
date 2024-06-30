package frontend.ir.instr.binop;

import frontend.ir.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class SDivInstr extends BinaryOperation {
    public SDivInstr(int result, Value op1, Value op2, BasicBlock parentBB) {
        super(result, op1, op2, "sdiv", DataType.INT, parentBB);
        assert op1.getDataType() == DataType.INT;
        assert op2.getDataType() == DataType.INT;
    }
}
