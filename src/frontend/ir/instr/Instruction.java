package frontend.ir.instr;

import Utils.CustomList;
import frontend.ir.Value;

public abstract class Instruction extends CustomList.Node<Instruction> implements Value {
    public abstract String print();
}
