package frontend.ir;

public enum DataType {
    INT("i32"),
    FLOAT("float"),
    VOID("void"),
    BOOL("i1");   // 1 位整数
    
    private final String name;
    private boolean pointer;
    
    DataType(String name) {
        this.name = name;
        pointer = false;
    }
    
    public void setPointer() {
        pointer = true;
    }
    
    public boolean isPointer() {
        return pointer;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}


