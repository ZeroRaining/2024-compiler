package frontend.ir.instr.otherop.cmp;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

public abstract class Cmp extends Instruction {
    protected final int result;
    protected final CmpCond cond;
    protected Value op1;
    protected Value op2;
    
    public Cmp(int result, CmpCond cond, Value op1, Value op2, BasicBlock parentBB) {
        super(parentBB);
        this.result = result;
        this.cond = cond;
        this.op1 = op1;
        this.op2 = op2;
        setUse(op1);
        setUse(op2);
    }
    
    @Override
    public Number getNumber() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    @Override
    public void modifyValue(Value from, Value to) {
        boolean notOk = true;
        if (op1 == from) {
            op1 = to;
            notOk = false;
        }
        if (op2 == from) {
            op2 = to;
            notOk = false;
        }
        if (notOk) {
            throw new RuntimeException();
        }
    }
}
