package frontend.ir.instr.otherop.cmp;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

public abstract class Cmp extends Instruction {
    protected final int result;
    protected final CmpCond cond;
    protected final Value op1;
    protected final Value op2;
    
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
    public Number getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }
}
