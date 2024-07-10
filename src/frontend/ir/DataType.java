package frontend.ir;

public enum DataType {
    INT("i32"),
    FLOAT("float"),
    VOID("void"),
    VOID_("i8*"),  // void*, 通用的指针
    BOOL("i1"),   // 1 位整数
    LONG_INT("i64");    // 64 位整数，用于指针运算
    
    private final String name;
    
    DataType(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}


