package midend;

import frontend.ir.DataType;
import frontend.ir.instr.memop.GEPInstr;
import frontend.ir.structure.FParam;
import frontend.ir.structure.Function;

import java.util.ArrayList;
import java.util.List;

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
        List<FParam> fParams = function.getFParams();
        if (fParams.isEmpty()) {
            return false;   // 没有参数的函数不能记忆化
        }
        for (FParam param : fParams) {
            if (param.getPointerLevel() > 0) {
                return false;   // 如果传了指针参数则不可以记忆化
            }
        }
        
        // 只有调用自身多次做记忆化才有意义，而且函数执行过程中不能有副作用，也不能有 load todo: 检查一下这里的条件
        return function.mightBeAbleToBeMemorized();
    }
}
