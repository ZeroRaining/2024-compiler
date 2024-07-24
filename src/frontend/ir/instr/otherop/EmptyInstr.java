package frontend.ir.instr.otherop;

import debug.DEBUG;
import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

public class EmptyInstr extends Instruction {
    private static int cnt = 0;
    private int myCnt;
    private DataType dataType;

    public EmptyInstr(DataType dataType) {
        this.dataType = dataType;
        myCnt = cnt++;
    }
    
    @Override
    public Number getNumber() {
        return null;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String value2string() {
        if (DEBUG.debug1) {
            return "%empty_" + myCnt;
        }
        throw new RuntimeException("空指令不应该剩下");
    }

    @Override
    public String print() {
        return null;
    }

    @Override
    public void modifyValue(Value from, Value to) {
        throw new RuntimeException("占位空指令没有可以替换的 value,也不应该进入替换流程");
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
