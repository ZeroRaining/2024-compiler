package frontend.ir.instr.binop;

import frontend.ir.constvalue.ConstInt;
import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.ArrayList;

public class SubInstr extends BinaryOperation {
    public SubInstr(int result, Value op1, Value op2) {
        super(result, op1, op2, "sub", DataType.INT);
        assert op1.getDataType() == DataType.INT;
        assert op2.getDataType() == DataType.INT;
    }
    
    @Override
    public Value operationSimplify() {
        if (op1 instanceof ConstInt && op2 instanceof ConstInt) {
            return new ConstInt(((ConstInt) op1).getNumber() - ((ConstInt) op2).getNumber());
        } else if (op2 instanceof ConstInt) {
            if (op2.getNumber().intValue() == 0) {
                return op1;
            }
            if (op1 instanceof AddInstr) {
                ArrayList<Value> userSet2list = new ArrayList<>(op1.getUserSet());
                if (userSet2list.size() == 1 && userSet2list.get(0) == this) {
                    Value upperOp1 = ((AddInstr) op1).getOp1();
                    Value upperOp2 = ((AddInstr) op1).getOp2();
                    if (upperOp1 instanceof ConstInt) {
                        ((ConstInt) upperOp1).sub(op2.getNumber().intValue());
                        return op1;
                    }
                    if (upperOp2 instanceof ConstInt) {
                        ((ConstInt) upperOp2).sub(op2.getNumber().intValue());
                        return op1;
                    }
                }
            } else if (op1 instanceof SubInstr) {
                ArrayList<Value> userSet2list = new ArrayList<>(op1.getUserSet());
                if (userSet2list.size() == 1 && userSet2list.get(0) == this) {
                    Value upperOp1 = ((SubInstr) op1).getOp1();
                    Value upperOp2 = ((SubInstr) op1).getOp2();
                    if (upperOp1 instanceof ConstInt) {
                        ((ConstInt) upperOp1).sub(op2.getNumber().intValue());
                        return op1;
                    }
                    if (upperOp2 instanceof ConstInt) {
                        ((ConstInt) upperOp2).add(op2.getNumber().intValue());
                        return op1;
                    }
                }
            }
        }
        return null;
    }
}
