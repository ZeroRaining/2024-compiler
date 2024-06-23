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
}
