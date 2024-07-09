package frontend.ir.instr.binop;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class SDivInstr extends BinaryOperation {
    public boolean is64 = false;
    public SDivInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "sdiv", DataType.INT);
        assert op1.getDataType() == DataType.INT;
        assert op2.getDataType() == DataType.INT;
    }
}
