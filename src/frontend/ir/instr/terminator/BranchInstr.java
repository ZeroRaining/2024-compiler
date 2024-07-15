package frontend.ir.instr.terminator;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

public class BranchInstr extends Instruction {
    private Value condition;
    private BasicBlock thenTarget;
    private BasicBlock elseTarget;

    public BranchInstr(Value cond, BasicBlock thenTarget, BasicBlock elseTarget) {
        this.condition = cond;
        this.thenTarget = thenTarget;
        this.elseTarget = elseTarget;
        setUse(thenTarget);
        setUse(elseTarget);
        setUse(cond);
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
    public Number getNumber() {
        throw new RuntimeException("no value in branch");
    }

    @Override
    public DataType getDataType() {
        throw new RuntimeException("no data type");
    }

    @Override
    public String print() {
        return "br " + getCond().getDataType() + " " + condition.value2string() +
                ", label %" + getThenTarget().value2string() + ", label %" + getElseTarget().value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (condition == from) {
            condition = to;
        } else {
            throw new RuntimeException();
        }
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
}

