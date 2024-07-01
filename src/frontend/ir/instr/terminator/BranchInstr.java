package frontend.ir.instr.terminator;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

public class BranchInstr extends Instruction {
    private Value condition;
    private BasicBlock thenTarget;
    private BasicBlock elseTarget;

    public BranchInstr(Value cond, BasicBlock thenTarget, BasicBlock elseTarget, BasicBlock parent) {
        super(parent);
        this.condition = cond;
        this.thenTarget = thenTarget;
        this.elseTarget = elseTarget;
    }

    @Override
    public String toString() {
        return "br " + getCond().getDataType() +" "+condition.value2string()+ ", label %" + getThenTarget().getLabelCnt() + ", label %" + getElseTarget().getLabelCnt();
    }

    public Value getCond() {
        return condition;
    }

    public BasicBlock getThenTarget() {
        return thenTarget;
    }

    public BasicBlock getElseTarget() {
        return elseTarget;
    }

    @Override
    public Number getValue() {
        return null;
    }

    @Override
    public DataType getDataType() {
        return null;
    }

    @Override
    public String print() {
        return null;
    }
}

