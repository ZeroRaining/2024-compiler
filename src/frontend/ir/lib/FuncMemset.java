package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.List;

public class FuncMemset extends LibFunc {
    public FuncMemset(List<Value> rParams) {
        super(rParams);
    }
    
    @Override
    public String getName() {
        return "memset";
    }
    
    @Override
    public String printDeclaration() {
        return "declare void @memset(i32*, i32, i32)";
    }
    
    @Override
    protected DataType getType() {
        return DataType.VOID;
    }
    
    @Override
    protected boolean checkParams(List<Value> rParams) {
        if (rParams == null) {
            throw new NullPointerException();
        }
        if (rParams.size() != 3) {
            return false;
        }
        DataType type0 = rParams.get(0).getDataType();
        DataType type1 = rParams.get(1).getDataType();
        DataType type2 = rParams.get(2).getDataType();
        if (rParams.get(0).getPointerLevel() != 1) {
            return false;
        }
        if (type0 != DataType.INT && type0 != DataType.FLOAT) {
            return false;
        }
        return type1 == DataType.INT && type2 == DataType.INT;
    }
}
