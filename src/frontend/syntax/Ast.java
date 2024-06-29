//TODO: 暂用于parser

package frontend.syntax;

import frontend.ir.DataType;
import frontend.lexer.Token;
import frontend.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有的语法树节点
 * 为简化编译器实现难度, 对文法进行了改写(不影响语义)
 */
public class Ast {

    public ArrayList<CompUnit> nodes;
    boolean printPermission;

    // CompUnit -> Decl | FuncDef
    public interface CompUnit {
    }

    // Decl -> ['const'] 'int' Def {',' Def} ';'
    public static class Decl implements CompUnit, BlockItem {

        public boolean constant;
        public Token type;
        public ArrayList<Def> defs;

        public Decl(boolean constant, Token bType, ArrayList<Def> defs) {
            this.constant = constant;
            this.type = bType;
            this.defs = defs;
            assert bType != null;
            assert defs != null;
        }
    }

    // Def -> Ident {'[' Exp ']'} ['=' Init]
    public static class Def {

        public TokenType type;
        public Token ident;
        public ArrayList<Exp> indexList;
        public Init init;

        public Def(TokenType type, Token ident, ArrayList<Exp> indexList, Init init) {
            this.type = type;
            this.ident = ident;
            this.indexList = indexList;
            this.init = init;
            assert type != null;
            assert ident != null;
            assert indexList != null;
        }
    }

    // Init -> Exp | InitArray
    public interface Init {
    }

    // InitArray -> '{' [ Init { ',' Init } ] '}'
    public static class InitArray implements Init {
        public ArrayList<Init> initList;

        public InitArray(ArrayList<Init> initList) {
            this.initList = initList;
            assert initList != null;
        }
    }

    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    // FuncFParams -> FuncFParam {',' FuncFParam}
    public static class FuncDef implements CompUnit {

        public Token type; // FuncType
        public Token ident; // name
        public ArrayList<FuncFParam> params;
        public Block body;

        public FuncDef(Token type, Token ident, ArrayList<FuncFParam> params, Block body) {
            this.type = type;
            this.ident = ident;
            this.params = params;
            this.body = body;
            assert type != null;
            assert ident != null;
            assert params != null;
            assert body != null;
        }
        
        public Token getIdent() {
            return ident;
        }
        
        public Token getType() {
            return type;
        }
        
        public ArrayList<FuncFParam> getFParams() {
            return params;
        }
        
        public Block getBody() {
            return body;
        }
    }

    // FuncFParam -> BType Ident ['[' ']' { '[' Exp ']' }]
    public static class FuncFParam {

        public Token type;
        public Token ident;
        public boolean array; // whether it is an array
        public ArrayList<Exp> arrayItemList; // array sizes of each dim

        public FuncFParam(Token type, Token ident, boolean array, ArrayList<Exp> arrayItemList) {
            this.type = type;
            this.ident = ident;
            this.array = array;
            this.arrayItemList = arrayItemList;
            assert type != null;
            assert ident != null;
            assert arrayItemList != null;
        }
    }

    // Block
    public static class Block implements Stmt {
        public ArrayList<BlockItem> items;

        public Block(ArrayList<BlockItem> items) {
            this.items = items;
            assert items != null;
        }
        
        public ArrayList<BlockItem> getItems() {
            return items;
        }
    }

    // BlockItem -> Decl | Stmt
    public interface BlockItem {
    }

    // Stmt -> Assign | ExpStmt | Block | IfStmt | WhileStmt | Break | Continue | Return
    public interface Stmt extends BlockItem {
    }

    // Assign
    public static class Assign implements Stmt {

        public LVal left;
        public Exp right;

        public Assign(LVal left, Exp right) {
            this.left = left;
            this.right = right;
            assert left != null;
            assert right != null;
        }
    }

    // ExpStmt
    public static class ExpStmt implements Stmt {
        public Exp exp; // nullable, empty stmt if null

        public ExpStmt(Exp exp) {
            this.exp = exp;
        }
    }

    // IfStmt
    public static class IfStmt implements Stmt {

        public Exp condition;
        public Stmt thenStmt;
        public Stmt elseStmt;

        public IfStmt(Exp condition, Stmt thenTarget, Stmt elseTarget) {
            this.condition = condition;
            this.thenStmt = thenTarget;
            this.elseStmt = elseTarget;
            assert condition != null;
            assert thenTarget != null;
        }
    }

    // WhileStmt
    public static class WhileStmt implements Stmt {

        public Exp cond;
        public Stmt body;

        public WhileStmt(Exp cond, Stmt body) {
            this.cond = cond;
            this.body = body;
            assert cond != null;
            assert body != null;
        }
    }

    // Break
    public static class Break implements Stmt {
        public Break() {
        }
    }

    // Continue
    public static class Continue implements Stmt {
        public Continue() {
        }
    }

    // Return
    public static class Return implements Stmt {
        public Exp returnValue;

        public Return(Exp returnValue) {
            this.returnValue = returnValue;
        }
        
        public Exp getReturnValue() {
            return returnValue;
        }
    }

    // PrimaryExp -> Call | '(' Exp ')' | LVal | Number
    // Init -> Exp | InitArray
    // Exp -> BinaryExp | UnaryExp
    public interface Exp extends Init, PrimaryExp {
        DataType checkConstType();  // 约定：如果表达式不是常量，返回 VOID；如果是常量，则返回对应的数据类型
        Integer getConstInt();
        Float getConstFloat();
    }

    // BinaryExp: Arithmetic, Relation, Logical
    // BinaryExp -> Exp { Op Exp }, calc from left to right
    public static class BinaryExp implements Exp {

        public Exp firstExp;
        public ArrayList<Token> ops;
        public ArrayList<Exp> RestExps;

        public BinaryExp(Exp firstExp, ArrayList<Token> ops, ArrayList<Exp> RestExps) {
            this.firstExp = firstExp;
            this.ops = ops;
            this.RestExps = RestExps;
            assert firstExp != null;
            assert ops != null;
            assert RestExps != null;
        }

        public Exp getFirstExp() {
            return firstExp;
        }
        public ArrayList<Exp> getRestExps() {
            return RestExps;
        }
        
        @Override
        public DataType checkConstType() {
            DataType constType = firstExp.checkConstType();
            if (constType == DataType.VOID) {
                return DataType.VOID;
            }
            for (Exp exp : RestExps) {
                switch (exp.checkConstType()) {
                    case VOID: return DataType.VOID;
                    case FLOAT: constType = DataType.FLOAT;
                }
            }
            return constType;
        }
        
        @Override
        public Integer getConstInt() {
            Integer res = firstExp.getConstInt();
            if (res == null) {
                return null;
            }
            for (int i = 0; i < RestExps.size(); i++) {
                Exp exp = RestExps.get(i);
                Token op = ops.get(i);
                Integer num = exp.getConstInt();
                if (num == null) {
                    return null;
                }
                switch (op.getType()) {
                    case ADD: res += num; break;
                    case SUB: res -= num; break;
                    case MUL: res *= num; break;
                    case DIV: res /= num; break;
                    case MOD: res %= num; break;
                    default: throw new RuntimeException("整数常量表达式中出现了未曾设想的运算符");
                }
            }
            return res;
        }
        
        @Override
        public Float getConstFloat() {
            Float res = firstExp.getConstFloat();
            if (res == null) {
                return null;
            }
            for (int i = 0; i < RestExps.size(); i++) {
                Exp exp = RestExps.get(i);
                Token op = ops.get(i);
                Float num = exp.getConstFloat();
                if (num == null) {
                    return null;
                }
                switch (op.getType()) {
                    case ADD: res += num; break;
                    case SUB: res -= num; break;
                    case MUL: res *= num; break;
                    case DIV: res /= num; break;
                    case MOD: res %= num; break;
                    default: throw new RuntimeException("浮点数常量表达式中出现了未曾设想的运算符");
                }
            }
            return res;
        }
    }

    // UnaryExp -> {UnaryOp} PrimaryExp
    public static class UnaryExp implements Exp {

        public ArrayList<Token> ops;
        public PrimaryExp primary;

        public UnaryExp(ArrayList<Token> ops, PrimaryExp primary) {
            this.ops = ops;
            this.primary = primary;
            assert ops != null;
            assert primary != null;
        }
        public ArrayList<Token> getUnaryOps() {
            return ops;
        }
        public PrimaryExp getPrimaryExp() {
            return primary;
        }
        
        @Override
        public DataType checkConstType() {
            if (primary instanceof Exp) {
                return ((Exp) primary).checkConstType();
            } else if (primary instanceof Call) {
                return DataType.VOID;
            } else if (primary instanceof LVal) {
                // todo : 这里应该判断这个左值是不是常量
                return DataType.VOID;
            } else if (primary instanceof Number) {
                if (((Number) primary).isIntConst) {
                    return DataType.INT;
                } else if (((Number) primary).isFloatConst) {
                    return DataType.FLOAT;
                } else {
                    throw new RuntimeException("出现了未定义的数值常量类型");
                }
            } else {
                throw new RuntimeException("出现了未定义的基本表达式");
            }
        }
        
        @Override
        public Integer getConstInt() {
            int sign = 1;
            for (Token op : ops) {
                if (op.getType() == TokenType.SUB) {
                    sign *= -1;
                } else if (op.getType() != TokenType.ADD) {
                    throw new RuntimeException("出现了意料之外的符号");
                }
            }
            if (primary instanceof Exp) {
                return ((Exp) primary).getConstInt() * sign;
            } else if (primary instanceof Call) {
                return null;
            } else if (primary instanceof LVal) {
                // todo : 这里应该判断这个左值是不是常量
                return null;
            } else if (primary instanceof Number) {
                if (((Number) primary).isIntConst) {
                    return ((Number) primary).getIntConstValue() * sign;
                } else if (((Number) primary).isFloatConst) {
                    return null;
                } else {
                    throw new RuntimeException("出现了未定义的数值常量类型");
                }
            } else {
                throw new RuntimeException("出现了未定义的基本表达式");
            }
        }
        
        @Override
        public Float getConstFloat() {
            int sign = 1;
            for (Token op : ops) {
                if (op.getType() == TokenType.SUB) {
                    sign *= -1;
                } else if (op.getType() != TokenType.ADD) {
                    throw new RuntimeException("出现了意料之外的符号");
                }
            }
            if (primary instanceof Exp) {
                return ((Exp) primary).getConstFloat() * sign;
            } else if (primary instanceof Call) {
                return null;
            } else if (primary instanceof LVal) {
                // todo : 这里应该判断这个左值是不是常量
                return null;
            } else if (primary instanceof Number) {
                if (((Number) primary).isIntConst) {
                    return (float) ((Number) primary).getIntConstValue() * sign;
                } else if (((Number) primary).isFloatConst) {
                    return ((Number) primary).getFloatConstValue() * sign;
                } else {
                    throw new RuntimeException("出现了未定义的数值常量类型");
                }
            } else {
                throw new RuntimeException("出现了未定义的基本表达式");
            }
        }
    }

    // PrimaryExp -> Call | '(' Exp ')' | LVal | Number
    public interface PrimaryExp {
    }

    // LVal -> Ident {'[' Exp ']'}
    public static class LVal implements PrimaryExp {

        public Token ident;
        public ArrayList<Exp> indexList;

        public LVal(Token ident, ArrayList<Exp> indexList) {
            this.ident = ident;
            this.indexList = indexList;
            assert ident != null;
            assert indexList != null;
        }
    }

    // Number
    public static class Number implements PrimaryExp {

        private Token number;
        private boolean isIntConst = false;
        private boolean isFloatConst = false;
        private int intConstValue = 0;
        private float floatConstValue = (float) 0.0;

        public Number(Token number) {
            assert number != null;
            this.number = number;

            if (number.isIntConst()) {
                isIntConst = true;
                // todo: 这里原本是高级 switch
                switch (number.getType()) {
                    case HEX_INT :
                        intConstValue = Integer.parseInt(number.getContent().substring(2), 16);
                        break;
                    case OCT_INT :
                        intConstValue = Integer.parseInt(number.getContent().substring(1), 8);
                        break;
                    case DEC_INT :
                        intConstValue = Integer.parseInt(number.getContent());
                        break;
                    default:
                        throw new AssertionError("Bad Number!");
                }
                floatConstValue = (float) intConstValue;
            } else if (number.isFloatConst()) {
                isFloatConst = true;
                floatConstValue = Float.parseFloat(number.getContent());
                intConstValue = (int) floatConstValue;
            } else {
                assert isIntConst || isFloatConst;
            }
        }
        
        public boolean isIntConst() {
            return isIntConst;
        }
        
        public boolean isFloatConst() {
            return isFloatConst;
        }
        
        public int getIntConstValue() {
            return intConstValue;
        }
        
        public float getFloatConstValue() {
            return floatConstValue;
        }
    }

    // Call -> Ident '(' [ Exp {',' Exp} ] ')'
    // FuncRParams -> Exp {',' Exp}, already inlined in Call
    public static class Call implements PrimaryExp {

        public Token ident;
        public ArrayList<Exp> params;

        public Call(Token ident, ArrayList<Exp> params) {
            assert ident != null;
            assert params != null;
            this.ident = ident;
            this.params = params;
        }
    }

    public Ast(ArrayList<CompUnit> nodes) {
        assert nodes != null;
        this.nodes = nodes;
    }
    
    public List<CompUnit> getUnits() {
        return this.nodes;
    }
}
