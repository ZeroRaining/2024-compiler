package frontend.ir.symbols;

import frontend.ir.DataType;
import frontend.ir.Value;
import frontend.ir.constvalue.ConstFloat;
import frontend.ir.constvalue.ConstInt;

import java.util.ArrayList;
import java.util.List;

public class ArrayInitVal extends Value {
    private final DataType dataType;
    private final ArrayList<Value> initList = new ArrayList<>();
    private final List<Integer> limList;
    
    public ArrayInitVal(DataType dataType, List<Integer> limList) {
        this.dataType = dataType;
        this.limList = limList;
    }
    
    public void addInitValue(Value newVal) {
        if (newVal == null) {
            throw new NullPointerException();
        }
        if (newVal instanceof ArrayInitVal) {
            int dim = ((ArrayInitVal) newVal).getDim();
            if (dim < this.getDim() - 1) {
                int len = limList.size();
                List<Integer> nextLimList = limList.subList(len - 1 - dim, len);
                ArrayInitVal newInit = new ArrayInitVal(dataType, nextLimList);
                newInit.addInitValue(newVal);
                addInitValue(newInit);
            } else {
                initList.add(newVal);
            }
        } else if (this.getDim() == 1) {
            initList.add(newVal);
        } else {
            ArrayList<Integer> nextLimList = new ArrayList<>();
            nextLimList.add(limList.get(limList.size() - 1));
            ArrayInitVal newInit = new ArrayInitVal(dataType, nextLimList);
            newInit.addInitValue(newVal);
            addInitValue(newInit);
        }
        
    }
    
    public Value getValueWithIndex(List<Integer> indexList) {
        if (indexList == null) {
            throw new NullPointerException();
        }
        int index = indexList.get(0);
        if (index >= limList.get(0)) {
            throw new RuntimeException("越界访问");
        }
        
        if (indexList.get(0) >= initList.size()) {
            switch (dataType) {
                case INT:   return new ConstInt(0);
                case FLOAT: return new ConstFloat(0);
                default: throw new RuntimeException("?");
            }
        }
        Value init = initList.get(indexList.get(0));
        if (indexList.size() > 1) {
            if (init instanceof ArrayInitVal) {
                return ((ArrayInitVal) init).getValueWithIndex(indexList.subList(1, indexList.size()));
            } else {
                throw new RuntimeException("非基层数组不是数组？");
            }
        } else {
            return init;
        }
    }
    
    private int getDim() {
        return this.limList.size();
    }
    
    @Override
    public Number getNumber() {
        throw new RuntimeException("占位类，该方法不应该被调用");
    }
    
    @Override
    public DataType getDataType() {
        return dataType;
    }
    
    @Override
    public String value2string() {
        if (initList.isEmpty()) {
            return "zeroinitializer";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (limList.size() == 1) {
            boolean hasNonZero = false;
            for (Value init : initList) {
                if (init.getNumber().intValue() != 0) {
                    hasNonZero = true;
                    break;
                }
            }
            if (hasNonZero) {
                stringBuilder.append("[");
                int lim = limList.get(0);
                for (int i = 0; i < lim; i++) {
                    Value value;
                    if (i < initList.size()) {
                        value = initList.get(i);
                    } else {
                        switch (dataType) {
                            case INT:   value = new ConstInt(0);    break;
                            case FLOAT: value = new ConstFloat(0);  break;
                            default: throw new RuntimeException();
                        }
                    }
                    stringBuilder.append(dataType).append(" ").append(value.value2string());
                    if (i < lim - 1) {
                        stringBuilder.append(", ");
                    }
                }
                stringBuilder.append("]");
            } else {
                return "zeroinitializer";
            }
        } else {
            stringBuilder.append("[");
            for (int i = 0; i < limList.get(0); i++) {
                if (i < initList.size()) {
                    Value init = initList.get(i);
                    if (init instanceof ArrayInitVal) {
                        stringBuilder.append(((ArrayInitVal) init).printArrayTypeName()).append(" ");
                        stringBuilder.append(init.value2string());
                    } else {
                        throw new RuntimeException("非基层数组里出现单蹦元素");
                    }
                } else {
                    stringBuilder.append(((ArrayInitVal) initList.get(0)).printArrayTypeName());
                    stringBuilder.append(" zeroinitializer");
                }
                if (i < limList.get(0) - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("]");
        }
        
        
        return stringBuilder.toString();
    }
    
    public String printArrayTypeName() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Integer index : limList) {
            stringBuilder.append("[").append(index).append(" x ");
        }
        stringBuilder.append(dataType);
        for (int i = 0; i < limList.size(); i++) {
            stringBuilder.append("]");
        }
        return stringBuilder.toString();
    }
}
