package backend.itemStructure;

import java.util.ArrayList;

public class AsmGlobalVar {
    public String name;
    private int size;
    private ArrayList<Integer> elements;
    private static int lcCnt = 0;
    public AsmGlobalVar(String name, ArrayList<Integer> elements) {
        this.name = name;
        this.size = 4 * elements.size();
        this.elements = elements;
    }

    public AsmGlobalVar(String name, int size) {
        this.name = name;
        this.size = size;
        this.elements = null;
    }

    public AsmGlobalVar(int floatvar) {
        this.name = ".LC" + lcCnt;
        lcCnt++;
        this.size = 4;
        this.elements = new ArrayList<>();
        this.elements.add(floatvar);
    }
}
