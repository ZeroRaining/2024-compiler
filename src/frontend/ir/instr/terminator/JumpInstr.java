package frontend.ir.instr.terminator;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.instr.Instruction;

public class JumpInstr extends Instruction {
    private BasicBlock Target;

    public JumpInstr(BasicBlock Target, BasicBlock parent) {
        super(parent);
        this.Target = Target;
        setUse(Target);
    }

    public BasicBlock getTarget() {
        return Target;
    }

    @Override
    public Integer getValue() {
        return -1;
    }

    @Override
    public DataType getDataType() {
        throw new RuntimeException("no data type");
    }

    @Override
    public String print() {
        return "br label " + Target.value2string();
    }
}
