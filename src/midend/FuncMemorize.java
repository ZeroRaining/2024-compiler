package midend;

import frontend.ir.DataType;
import frontend.ir.structure.Function;

import java.util.ArrayList;

/**
 * （递归）函数记忆化，通过全局数组减少递归运算
 */
public class FuncMemorize {
    public static void execute(ArrayList<Function> functions) {
    
    }
    
    private static boolean canBeMemorized(Function function) {
        if (function.getDataType() == DataType.VOID) {
            return false;   // 如果没有返回值不能记忆化
        }
        
    }
}
