package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.List;

public class FuncPutch extends LibFunc {
    public FuncPutch(List<Value> rParams) {
        super(rParams);
    }
    
    @Override
    public String getName() {
        return "putch";
    }
    
    @Override
    public String printDeclaration() {
        return "declare void @putch(i32)";
    }
    
    @Override
    public DataType getType() {
        return DataType.VOID;
    }
    
    @Override
    protected boolean checkParams(List<Value> rParams) {
        return rParams != null && rParams.size() == 1 && rParams.get(0).getDataType() == DataType.INT;
    }
}
