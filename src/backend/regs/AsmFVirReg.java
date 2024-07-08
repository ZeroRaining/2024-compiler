package backend.regs;

public class AsmFVirReg extends AsmReg{
    private static int index;
    public AsmFVirReg() {
        index++;
    }
    public String toString() {
        return "fvir" + index;
    }
}
