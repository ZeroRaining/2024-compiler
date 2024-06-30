package backend.regs;

public class AsmPhyReg {
    private int index;
    private String name;
    public AsmPhyReg(String name) {
        this.index = RegGeter.nameToIndexInt.get(name);
        this.name = name;
    }
}
