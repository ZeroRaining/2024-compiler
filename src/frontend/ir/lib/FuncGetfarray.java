package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.List;

public class FuncGetfarray extends LibFunc {
    public FuncGetfarray(List<Value> rParams) {
        super(rParams);
    }
    
    @Override
    public String getName() {
        return "getfarray";
    }
    
    @Override
    public String printDeclaration() {
        return "declare i32 @getfarray(float*)";
    }
    
    @Override
    protected DataType getType() {
        return DataType.INT;
    }
    
    @Override
    protected boolean checkParams(List<Value> rParams) {
        return  rParams != null &&
                rParams.size() == 1 &&
                rParams.get(0).getPointerLevel() == 1 &&
                rParams.get(0).getDataType() == DataType.FLOAT;
    }
}
