package frontend.ir.instr;

import frontend.ir.DataType;

public class ReturnInstr implements Instruction {
    private DataType returnType;
    private final Integer retI;
    private final Float retF;
    
    public ReturnInstr(DataType returnType) {
        this.returnType = returnType;
        if (returnType != DataType.VOID) {
            throw new RuntimeException("在应该返回值的函数中没有返回值");
        }
        retI = null;
        retF = null;
    }
    
    public ReturnInstr(DataType returnType, int ret) {
        this.returnType = returnType;
        if (returnType == DataType.VOID) {
            throw new RuntimeException("在不该返回值的函数中返回值了");
        }
        if (returnType == DataType.FLOAT) {
            retI = null;
            retF = (float) ret;
        } else {
            retI = ret;
            retF = null;
        }
    }
    
    public ReturnInstr(DataType returnType, float ret) {
        this.returnType = returnType;
        if (returnType == DataType.VOID) {
            throw new RuntimeException("在不该返回值的函数中返回值了");
        }
        if (returnType == DataType.FLOAT) {
            retI = null;
            retF = ret;
        } else {
            retI = (int) ret;
            retF = null;
        }
    }
    
    @Override
    public int getResultIndex() {
        return -1;
    }
    
    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder("ret ");
        switch (this.returnType) {
            case INT:
                stringBuilder.append("i32 ");
                if (retI != null) {
                    stringBuilder.append(retI);
                } else {
                    // todo 这里要考虑 symbol
                }
                break;
            case FLOAT:
                stringBuilder.append("float ");
                if (retF != null) {
                    stringBuilder.append(retF);
                } else {
                    // todo 这里要考虑 symbol
                }
                break;
            case VOID:
                stringBuilder.append("void");
                break;
            default:
                throw new RuntimeException("返回指令中出现了不存在的类型");
        }
        return stringBuilder.toString();
    }
}
