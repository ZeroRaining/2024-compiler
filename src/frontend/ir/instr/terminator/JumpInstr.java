package frontend.ir.instr.terminator;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class JumpInstr extends Instruction {
    private BasicBlock Target;

    public JumpInstr(BasicBlock Target, BasicBlock parent) {
        super(parent);
        this.Target = Target;
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
        return "br label %" + Target.getLabelCnt();
    }
}
