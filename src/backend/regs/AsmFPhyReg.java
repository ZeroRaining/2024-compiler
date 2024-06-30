package backend.regs;

public class AsmFPhyReg {
    private final int index;
    private final String name;
    public AsmFPhyReg(String name) {
        this.name = name;
        this.index = RegGeter.nameToIndexFloat.get(name);
    }
}
