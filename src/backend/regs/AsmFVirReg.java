package backend.regs;

public class AsmFVirReg extends AsmReg {
    private static int wholeIndex = 0;
    private int personalIndex = 0;

    public AsmFVirReg() {
        personalIndex = wholeIndex;
        wholeIndex++;
    }

    public String toString() {
        return "fvir" + personalIndex;
    }
}
