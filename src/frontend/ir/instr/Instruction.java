package frontend.ir.instr;

import frontend.ir.Value;

public interface Instruction extends Value {
    int getResultIndex();   // 没有 result 的就先返回 -1 吧
    String print();
}
