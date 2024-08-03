package midend.loop;

public enum BlockType {
    /*
    * TODO:
    *  多次循环，重复染色？
    *  每次构建前的清理？
    *
    * */
    HEADER,
    ENTERING,
    EXIT,
    LATCH,
    INLOOP,
    EXITING,
    OUTOFLOOP
}
