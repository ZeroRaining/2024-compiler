package frontend.ir.structure;

import frontend.ir.DataType;
import frontend.ir.Value;

public class FParam extends Value {
    private final int virtualReg;   // 表示存放这个参数值的虚拟寄存器号
    private final DataType dataType;
    
    public FParam(int virtualReg, DataType dataType) {
        this.virtualReg = virtualReg;
        this.dataType = dataType;
    }
    
    @Override
    public Number getValue() {
        return virtualReg;
    }
    
    @Override
    public DataType getDataType() {
        return dataType;
    }
    
    @Override
    public String value2string() {
        return "%" + virtualReg;
    }
}
