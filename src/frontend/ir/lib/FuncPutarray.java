package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.Value;

import java.util.List;

public class FuncPutarray extends LibFunc {
    public FuncPutarray(List<Value> rParams) {
        super(rParams);
    }
    
    @Override
    public String getName() {
        return "putarray";
    }
    
    @Override
    public String printDeclaration() {
        return "declare void @putarray(i32, i32*)";
    }
    
    @Override
    protected DataType getType() {
        return DataType.VOID;
    }
    
    @Override
    protected boolean checkParams(List<Value> rParams) {
        // 运行时库说明中强调该函数不检查参数合法性
        return true;
    }
}
