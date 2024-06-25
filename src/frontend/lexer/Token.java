package frontend.lexer;

public class Token {
    private final TokenType type;
    public final String content;
    public Token(final TokenType type, final String content) {
        this.type = type;
        this.content = content;
    }
    public TokenType getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }

    public boolean isIntConst() {
        return type.ordinal() <= TokenType.DEC_INT.ordinal()
                && type.ordinal() >= TokenType.HEX_INT.ordinal();
    }

    public boolean isFloatConst() {
        return type.ordinal() <= TokenType.DEC_FLOAT.ordinal()
                && type.ordinal() >= TokenType.HEX_FLOAT.ordinal();
    }
}
//