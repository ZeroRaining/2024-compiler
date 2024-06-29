package frontend.ir.symbols.initalvalue;

import frontend.ir.DataType;
import frontend.ir.symbols.SymTab;
import frontend.syntax.Ast;

public abstract class InitVal {
    public static InitVal createInitVal(DataType type, Ast.Init init, SymTab symTab) {
        if (init == null) {
            throw new RuntimeException("空定义");
        }
        if (init instanceof Ast.Exp) {
            switch (((Ast.Exp) init).checkConstType(symTab)) {
                case INT:
                    if (type == DataType.INT) {
                        return new IntInitVal(((Ast.Exp) init).getConstInt(symTab));
                    } else if (type == DataType.FLOAT) {
                        return new FloatInitVal(((Ast.Exp) init).getConstInt(symTab).floatValue());
                    } else {
                        throw new RuntimeException("你给我传了个什么鬼类型啊");
                    }
                case FLOAT:
                    if (type == DataType.INT) {
                        return new IntInitVal(((Ast.Exp) init).getConstFloat(symTab).intValue());
                    } else if (type == DataType.FLOAT) {
                        return new FloatInitVal(((Ast.Exp) init).getConstFloat(symTab));
                    } else {
                        throw new RuntimeException("你给我传了个什么鬼类型啊");
                    }
                default: throw new RuntimeException("初始值似乎不是常量");
            }
        } else if (init instanceof Ast.InitArray) {
            // todo
            return null;
        } else {
            throw new RuntimeException("奇怪的定义类型");
        }
    }
}
