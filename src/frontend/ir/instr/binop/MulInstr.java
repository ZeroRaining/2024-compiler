package frontend.ir.instr.binop;

import frontend.ir.constvalue.ConstInt;
import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.ArrayList;

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
        } else if (op1 instanceof ConstInt) {
            return mergeConst((ConstInt) op1, op2);
        } else if (op2 instanceof ConstInt) {
            return mergeConst((ConstInt) op2, op1);
        }
        return null;
    }
    
    private Value mergeConst(ConstInt constInt, Value nonConst) {
        if (constInt == null || nonConst == null) {
            throw new NullPointerException();
        }
        if (constInt.getNumber() == 0) {
            return constInt;
        }
        if (nonConst instanceof MulInstr) {
            ArrayList<Value> userSet2list = new ArrayList<>(nonConst.getUserSet());
            if (userSet2list.size() == 1 && userSet2list.get(0) == this) {
                Value upperOp1 = ((MulInstr) nonConst).getOp1();
                Value upperOp2 = ((MulInstr) nonConst).getOp2();
                if (upperOp1 instanceof ConstInt) {
                    ((ConstInt) upperOp1).mul(constInt.getNumber());
                    return nonConst;
                }
                if (upperOp2 instanceof ConstInt) {
                    ((ConstInt) upperOp2).mul(constInt.getNumber());
                    return nonConst;
                }
            }
        } else if (nonConst instanceof SDivInstr) {
            ArrayList<Value> userSet2list = new ArrayList<>(nonConst.getUserSet());
            if (userSet2list.size() == 1 && userSet2list.get(0) == this) {
                Value upperOp1 = ((SDivInstr) nonConst).getOp1();
                if (upperOp1 instanceof ConstInt) {
                    ((ConstInt) upperOp1).mul(constInt.getNumber());
                    return nonConst;
                }
            }
        }
        
        return null;
    }
}
