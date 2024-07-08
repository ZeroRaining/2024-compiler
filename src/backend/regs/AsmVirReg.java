package backend.regs;

public class AsmVirReg extends AsmReg{
    private static int wholeIndex = 0;
    private int personalIndex = 0;

    public AsmVirReg() {
        personalIndex = wholeIndex;
        wholeIndex++;
    }

    public String toString() {
        return "vir" + personalIndex;
    }
}
