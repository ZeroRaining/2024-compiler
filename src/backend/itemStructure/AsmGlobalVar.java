package backend.itemStructure;

import java.util.ArrayList;

public class AsmGlobalVar {
    public String name;
    private int size;
    private ArrayList<Integer> elements;
    public AsmGlobalVar(String name, ArrayList<Integer> elements) {
        this.name = name.substring(1);
        this.size = 4 * elements.size();
        this.elements = elements;
    }

    public AsmGlobalVar(String name, int size) {
        this.name = name.substring(1);
        this.size = size;
        this.elements = null;
    }
}
