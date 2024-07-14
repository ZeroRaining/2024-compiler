package backend.itemStructure;

public class AsmImm12 extends AsmOperand{
    private int value;
    public AsmImm12(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    public void addNewAllocSize(int newAllocSize) {
        value -= newAllocSize;
    }
    public String toString() {
        return String.valueOf(value);
    }
}
