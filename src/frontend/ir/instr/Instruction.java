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
        value.insertAtTail(use);
        useList.add(use);
        useValueList.add(value);
    }

    
    public abstract String print();
    
    public BasicBlock getParentBB() {
        return parentBB;
    }
    
    @Override
    public String value2string() {
        return "%reg_" + this.getNumber();
    }
//修改当前的指令的use，将原来对from的使用改为对to的使用
    public void modifyUse(Value from, Value to) {
        for (Use one: useList) {
            if (one.getUsed() == from) {
                one.removeFromList();
                useList.remove(one);
                useValueList.remove(from);
                useValueList.add(to);
                setUse(to);
                modifyValue(from, to);
                return;
            }
        }
    }
    public abstract void modifyValue(Value from, Value to);
}
