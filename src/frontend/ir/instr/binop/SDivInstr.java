package frontend.ir.instr.binop;

import frontend.ir.constvalue.ConstInt;
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
    
    @Override
    public Value operationSimplify() {
        if (op1 instanceof ConstInt && op2 instanceof ConstInt) {
            return new ConstInt(((ConstInt) op1).getNumber() / ((ConstInt) op2).getNumber());
        }
        return null;
    }
}
