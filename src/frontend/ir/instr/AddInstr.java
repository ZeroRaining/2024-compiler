package frontend.ir.instr;

import frontend.ir.DataType;

/**
 * result = op1 + op2
 */
public class AddInstr implements Instruction {
    private int result;
    private int op1;
    private int op2;
    private DataType type;
    
    @Override
    public int getResultIndex() {
        return result;
    }
    
    @Override
    public String print() {
        // todo
        return "这里还没写哦";
    }
}
