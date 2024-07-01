package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

import java.util.List;

public class CallInstr extends Instruction {
    private final Integer result;
    private final DataType returnType;
    private final List<Value> rParams;
    
    public CallInstr(Integer result, DataType returnType, List<Value> rParams ,BasicBlock parentBB) {
        super(parentBB);
        assert (returnType == DataType.VOID && result == null) || (returnType != DataType.VOID && result != null);
        if (rParams == null) {
            throw new NullPointerException();
        }
        this.result = result;
        this.returnType = returnType;
        this.rParams = rParams;
        for (Value value : rParams) {
            setUse(value);
        }
        // todo 函数也要作为 value 被 use，而且要考虑一下怎么处理库函数
    }
    
    
    @Override
    public Number getValue() {
        return result;
    }
    
    @Override
    public DataType getDataType() {
        return returnType;
    }
    
    @Override
    public String print() {
        // todo
        throw new RuntimeException("...");
    }
}
