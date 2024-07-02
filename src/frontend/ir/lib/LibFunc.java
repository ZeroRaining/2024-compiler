package frontend.ir.lib;

import frontend.ir.DataType;
import frontend.ir.FuncDef;
import frontend.ir.Value;
import frontend.ir.instr.otherop.CallInstr;
import frontend.ir.structure.BasicBlock;

import java.util.List;

public abstract class LibFunc implements FuncDef {
    protected final List<Value> rParams;
    
    public LibFunc(List<Value> rParams) {
        this.rParams = rParams;
    }
    
    public abstract String printDeclaration();
    
    protected abstract DataType getType();
    
    protected abstract boolean checkParams(List<Value> rParams);
    
    public CallInstr makeCall(int result, List<Value> rParams, BasicBlock curBlock) {
        if (!checkParams(rParams)) {
            throw new RuntimeException("形参实参不匹配");
        }
        DataType type = getType();
        if (type == DataType.VOID) {
            return new CallInstr(null, type, this, rParams, curBlock);
        } else {
            return new CallInstr(result, type, this, rParams, curBlock);
        }
    }
}
