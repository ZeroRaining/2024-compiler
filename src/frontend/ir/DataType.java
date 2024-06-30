package frontend.ir;

public enum DataType {
    INT,
    FLOAT,
    VOID;
    
    @Override
    public String toString() {
        switch (this) {
            case INT:   return "i32";
            case FLOAT: return "float";
            default:    return "void";
        }
    }
}


