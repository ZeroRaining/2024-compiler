package frontend.ir;

import java.util.HashMap;

public class SymTab {
    private final HashMap<String, Symbol> symbolMap = new HashMap<>();
    private final SymTab parent;
    
    public SymTab(SymTab p) {
        parent = p;
    }
    
    public SymTab() {
        parent = null;
    }
    
    public boolean hasSym(String sym) {
        return symbolMap.containsKey(sym) || (parent != null && parent.hasSym(sym));
    }
    
    public Symbol getSym(String sym) {
        if (symbolMap.containsKey(sym)) {
            return symbolMap.get(sym);
        }
        if (parent == null) {
            return null;
        }
        return parent.getSym(sym);
    }
}
