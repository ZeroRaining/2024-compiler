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
        return -1;
    }

    @Override
    public DataType getDataType() {
        throw new RuntimeException("no data type");
    }

    @Override
    public String print() {
        return "br " + getCond().getDataType() +" "+condition.value2string()+ ", label %" + getThenTarget().getLabelCnt() + ", label %" + getElseTarget().getLabelCnt();
    }
}

