package frontend.ir;

import Utils.CustomList;

public abstract class Value extends CustomList.Node {
    private static int value_num = 0;
    private static int id = ++value_num;
    /*begin与end是链表的头尾指针（本身不存放真正的use，也就是noUse时仅存在头尾指针），
    * 负责记录这个这个value被哪些use使用*/
    private Use beginUse = new Use();
    private Use endUse = new Use();
    public Value() {
        super();
        beginUse.setNext(endUse);
        endUse.setPrev(beginUse);
    }
    public void insertAtTail(Use use) {
        endUse.insertBefore(use);
    }
    public abstract Number getValue();
    public abstract DataType getDataType();
    public abstract String value2string();
    public Use getBeginUse() {
        return (Use) beginUse.getNext();
    }
    public Use getEndUse() {
        return (Use) endUse.getPrev();
    }

}
