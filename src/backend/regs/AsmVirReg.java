package backend.regs;

public class AsmVirReg extends AsmReg{
    private static int index;
    public AsmVirReg() {
        index++;
    }
    public String toString() {
        return "vir" + index;
    }
}
