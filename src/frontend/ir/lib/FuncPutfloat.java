package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.List;

public class FuncPutfloat extends LibFunc {
    public FuncPutfloat(List<Value> rParams) {
        super(rParams);
    }
    
    @Override
    public String getName() {
        return "putfloat";
    }
    
    @Override
    public String printDeclaration() {
        return "declare void @putfloat(float)";
    }
    
    @Override
    public DataType getType() {
        return DataType.VOID;
    }
    
    @Override
    protected boolean checkParams(List<Value> rParams) {
        return rParams != null && rParams.size() == 1 && rParams.get(0).getDataType() == DataType.FLOAT;
    }
}
