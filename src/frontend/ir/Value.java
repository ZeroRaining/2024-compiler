package frontend.ir;

import Utils.CustomList;

public abstract class Value extends CustomList.Node {
    public abstract Number getValue();
    public abstract DataType getDataType();
    public abstract String value2string();
}
