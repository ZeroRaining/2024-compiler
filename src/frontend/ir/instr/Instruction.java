package frontend.ir.instr;

import frontend.ir.Use;
import frontend.ir.structure.BasicBlock;
import frontend.ir.Value;

import java.util.ArrayList;

public abstract class Instruction extends Value {
    private final BasicBlock parentBB;
    protected ArrayList<Use> useList;
    protected ArrayList<Value> useValueList;
    public Instruction(BasicBlock parentBB) {
        super();
        this.parentBB = parentBB;
        this.useList = new ArrayList<>();
        this.useValueList = new ArrayList<>();
    }
    public void setUse(Value value) {
        Use use = new Use(this,value);
        value.inserAtTail(use);
        useList.add(use);
        useValueList.add(value);
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
