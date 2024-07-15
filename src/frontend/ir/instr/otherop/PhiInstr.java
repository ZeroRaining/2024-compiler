package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

import java.util.ArrayList;
import java.util.Objects;

public class PhiInstr extends Instruction {
    private final int result;
    private final DataType type;
    private ArrayList<Value> values;
    private ArrayList<BasicBlock> prtBlks;

    public PhiInstr(int result, DataType type, ArrayList<Value> values, ArrayList<BasicBlock> prtBlks) {
        this.result = result;
        this.type = type;
        this.values = values;
        this.prtBlks = prtBlks;
        for (Value value : values) {
            setUse(value);
        }
        //TODO: to set or not to set, is a question
        //TODO：刪除块时我的phi怎么办
//        for (BasicBlock block : prtBlks) {
//            setUse(block);
//        }
    }

    @Override
    public String value2string() {
        return "%phi_" + result;
    }

    @Override
    public Number getNumber() {
        throw new RuntimeException("Phi: There is no value!");
    }

    @Override
    public DataType getDataType() {
        return type;
    }

    @Override
    public String print() {
        StringBuilder ret = new StringBuilder(value2string() + " = phi " + getDataType() + " ");
        int len = values.size();
        for (int i = 0; i < len; i++) {
            Value value = values.get(i);
            //TODO：maybe异常处理？
            ret.append("[ ").append(value.value2string()).append(", ").append("%").append(prtBlks.get(i).value2string()).append(" ]");
            if (i < len - 1) {
                ret.append(", ");
            }
        }
        return ret.toString();
    }

    public void modifyUse(Value to, BasicBlock block) {
        int index = prtBlks.indexOf(block);
        Value from = values.get(index);
        super.modifyUse(from, to);
    }

    @Override
    public void modifyValue(Value from, Value to) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == from) {
                values.set(i, to);
            }
        }
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhiInstr)) {
            return false;
        }
        
        boolean checkNot1 = this.values.size() != ((PhiInstr) other).values.size();
        boolean checkNot2 = this.prtBlks.size() != ((PhiInstr) other).prtBlks.size();
        
        if (checkNot1 || checkNot2) {
            return false;
        }
        
        for (int i = 0; i < values.size(); i++) {
            if (!this.values.get(i).equals(((PhiInstr) other).values.get(i))) {
                return false;
            }
            if (!this.prtBlks.get(i).equals(((PhiInstr) other).prtBlks.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(values, prtBlks);
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
}
