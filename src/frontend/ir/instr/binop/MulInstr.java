package frontend.ir.instr.binop;

import frontend.ir.constvalue.ConstInt;
import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;

public class MulInstr extends BinaryOperation {
    public boolean is64 = false;
    public MulInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "mul", DataType.INT);
        assert op1.getDataType() == DataType.INT;
        assert op2.getDataType() == DataType.INT;
    }

    public void swapOp() {
        Value tmp = op1;
        op1 = op2;
        op2 = tmp;
    }
    
    @Override
    public Value operationSimplify() {
        if (op1 instanceof ConstInt && op2 instanceof ConstInt) {
            return new ConstInt(((ConstInt) op1).getNumber() * ((ConstInt) op2).getNumber());
        }
        return null;
    }
}
