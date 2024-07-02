package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;
import frontend.ir.structure.BasicBlock;

import java.util.List;

/**
 * 对于返回类型为 void 的函数，其 result 应为 null
 */
public class CallInstr extends Instruction {
    private final Integer result;
    private final DataType returnType;
    private final List<Value> rParams;
    private final FuncDef funcDef;
    
    public CallInstr(Integer result, DataType returnType, FuncDef funcDef, List<Value> rParams ,BasicBlock parentBB) {
        super(parentBB);
        assert (returnType == DataType.VOID && result == null) || (returnType != DataType.VOID && result != null);
        if (rParams == null) {
            throw new NullPointerException();
        }
        this.result = result;
        this.returnType = returnType;
        this.rParams = rParams;
        this.funcDef = funcDef;
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
        StringBuilder stringBuilder = new StringBuilder();
        if (result != null) {
            stringBuilder.append("%").append(result).append(" = ");
        }
        stringBuilder.append("call ").append(returnType);
        stringBuilder.append(" @").append(funcDef.getName()).append("(");
        for (Value value : rParams) {
            stringBuilder.append(value.getDataType()).append(" ").append(value.value2string());
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}