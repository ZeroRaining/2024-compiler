package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.Function;

public class MoveInstr extends Instruction {
    private Value src;
    private Value dst;

    public MoveInstr(Value src, Value dst) {
        this.src = src;
        this.dst = dst;
    }

    public Value getSrc() {
        return src;
    }

    public Value getDst() {
        return dst;
    }

    @Override
    public Number getNumber() {
        throw new RuntimeException("no number should be gotten");
    }

    @Override
    public DataType getDataType() {
        return src.getDataType();
    }

    @Override
    public String print() {
//        if (DEBUG.isMove) {
//            return "move " + getDataType() + " " + src.value2string() + " --> " + dst.value2string();
//        } else {
//            return dst.value2string() + " = add " + getDataType() + " 0, " + "%reg_" + src.value2string();
//        }
        return "move " + getDataType() + " " + src.value2string() + " --> " + dst.value2string();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        if (from == dst) {
            dst = from;
        } else if (to == dst) {
            dst = to;
        }
    }

    @Override
    public String myHash() {
        return "move " + getDataType() + " " + src.value2string() + " --> " + dst.value2string();
    }

    @Override
    public Value operationSimplify() {
        //TODO:
        return null;
    }
    
    @Override
    public Instruction cloneShell(Function parentFunc) {
        return new MoveInstr(src, dst);
    }
}
