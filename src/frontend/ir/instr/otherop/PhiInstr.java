package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.instr.memop.StoreInstr;
import frontend.ir.structure.BasicBlock;

import java.util.ArrayList;

public class PhiInstr extends Instruction {
    private final int result;
    private final DataType type;
    private ArrayList<Value> values;
    private ArrayList<BasicBlock> prtBlks;

    public PhiInstr(int result, ArrayList<Value> values, ArrayList<BasicBlock> prtBlks, BasicBlock parent) {
        super(parent);
        this.result = result;
        this.type = values.get(0).getDataType();
        this.values = values;
        this.prtBlks = prtBlks;
        for (Value value : values) {
            setUse(value);
        }
        //TODO : to set or not to set, is a question
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
            ret.append("[ ").append(((StoreInstr)value).getValue().value2string()).append(", ").append(prtBlks.get(i).value2string()).append(" ]");
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
            if (values.get(i) == to) {
                values.remove(i);
                values.add(i, to);
            }
        }
    }
}
