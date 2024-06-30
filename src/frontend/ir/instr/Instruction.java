package frontend.ir.instr;

import Utils.CustomList;
import frontend.ir.BasicBlock;
import frontend.ir.Value;

public abstract class Instruction extends CustomList.Node<Instruction> implements Value {
    private final BasicBlock parentBB;
    
    public Instruction(BasicBlock parentBB) {
        this.parentBB = parentBB;
    }
    
    public abstract String print();
    
    public BasicBlock getParentBB() {
        return parentBB;
    }
    
    @Override
    public String value2string() {
        return "%" + this.getValue();
    }
}
