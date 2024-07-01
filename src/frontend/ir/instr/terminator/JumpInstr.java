package frontend.ir.instr.terminator;

import frontend.ir.structure.BasicBlock;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.symbols.SymTab;

public class JumpInstr extends Instruction {
    private BasicBlock Target;

    public JumpInstr(BasicBlock Target, BasicBlock parent) {
        super(parent);
        this.Target = Target;
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
        return "br label %" + Target.getLabelCnt();
    }
}
