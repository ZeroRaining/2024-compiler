package midend.SSA;

import frontend.ir.structure.Function;

import java.util.HashSet;

public class Mem2Reg {
    private HashSet<Function> functions;
    public Mem2Reg(HashSet<Function> functions) {
        this.functions = functions;
        for (Function function : functions) {

        }
    }
}
