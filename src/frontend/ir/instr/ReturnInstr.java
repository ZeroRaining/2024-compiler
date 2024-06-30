package frontend.ir.instr;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstValue;

public class ReturnInstr implements Instruction {
    private final DataType returnType;
    private final Value returnValue;
    
    public ReturnInstr(DataType returnType) {
        if (returnType != DataType.VOID) {
            throw new RuntimeException("在应该返回值的函数中没有返回值");
        }
        this.returnType = returnType;
        returnValue = null;
    }
    
    public ReturnInstr(DataType returnType, Value returnValue) {
        if (returnType == DataType.VOID) {
            throw new RuntimeException("在不该返回值的函数中返回值了");
        }
        this.returnType = returnType;
        this.returnValue = returnValue;
    }
    
    @Override
    public Integer getValue() {
        return -1;
    }
    
    @Override
    public DataType getDataType() {
        return returnType;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder("ret ");
        stringBuilder.append(returnType).append(" ");
        switch (this.returnType) {
            case INT:
                if (returnValue instanceof ConstValue) {
                    stringBuilder.append(returnValue.getValue().intValue());
                } else if (returnValue instanceof Instruction) {
                    stringBuilder.append("%").append(returnValue.getValue().intValue());
                } else {
                    throw new RuntimeException("未曾设想的返回值");
                }
                break;
            case FLOAT:
                if (returnValue instanceof ConstValue) {
                    stringBuilder.append(returnValue.getValue().floatValue());
                } else if (returnValue instanceof Instruction) {
                    stringBuilder.append("%").append(returnValue.getValue().intValue());
                } else {
                    throw new RuntimeException("未曾设想的返回值");
                }
                break;
            case VOID:
                break;
            default:
                throw new RuntimeException("返回指令中出现了不存在的类型");
        }
        return stringBuilder.toString();
    }
}
