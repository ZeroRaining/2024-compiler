package frontend.ir.instr.otherop;

import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.instr.Instruction;

import java.util.List;

/**
 * 对于返回类型为 void 的函数，其 result 应为 null
 */
public class CallInstr extends Instruction {
    private final Integer result;
    private final DataType returnType;
    private final List<Value> rParams;
    private final FuncDef funcDef;
    
    public CallInstr(Integer result, DataType returnType, FuncDef funcDef, List<Value> rParams) {
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
    public Number getNumber() {
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
            stringBuilder.append("%reg_").append(result).append(" = ");
        }
        stringBuilder.append("call ").append(returnType);
        stringBuilder.append(" @").append(funcDef.getName()).append("(");
        int size = rParams.size();
        for (int i = 0; i < size; i++) {
            Value value = rParams.get(i);
            stringBuilder.append(value.type2string());
            stringBuilder.append(" ").append(value.value2string());
            if (i < size - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @Override
    public void modifyValue(Value from, Value to) {
        for (int i = 0; i < rParams.size(); i++) {
            Value value = rParams.get(i);
            if (value == from) {
                rParams.set(i, to);
                return;
            }
        }
        throw new RuntimeException("No such value");
    }
    
    @Override
    public String myHash() {
        return Integer.toString(this.hashCode());
    }
    
    public List<Value> getRParams() {
        return rParams;
    }

    public FuncDef getFuncDef() {
        return funcDef;
    }
    
    @Override
    public Value operationSimplify() {
        return null;
    }
}
