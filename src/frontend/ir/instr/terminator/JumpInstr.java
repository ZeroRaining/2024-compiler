package frontend.ir.instr.terminator;

import frontend.ir.Value;
import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.instr.Instruction;

public class JumpInstr extends Instruction {
    private BasicBlock Target;

    public JumpInstr(BasicBlock Target) {
        this.Target = Target;
        setUse(Target);
    }

    public void setRelation(BasicBlock prt) {
        prt.getSucs().add(Target);
        Target.getPres().add(prt);
    }

    @Override
    public void removeFromList() {
        if (this.getParentBB() == null) {
            throw new RuntimeException("why you dont have parent?");
        }
        BasicBlock prt = this.getParentBB();
        prt.getSucs().remove(Target);
        Target.getPres().remove(prt);
        super.removeFromList();
    }

    public BasicBlock getTarget() {
        return Target;
    }

    @Override
    public Integer getNumber() {
        return -1;
    }

    @Override
    public DataType getDataType() {
        throw new RuntimeException("no data type");
    }

    @Override
    public String print() {
        return "br label %" + Target.value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("没有可以置换的 value");
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
    
    @Override
    public String myHash() {
        return Integer.toString(this.hashCode());
    }
}
