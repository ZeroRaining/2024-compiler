package frontend.lexer;

import java.util.ArrayList;

public class TokenList {
    public final ArrayList<Token> tokens = new ArrayList<>();
    private int index = 0;
    public void append(Token token) {
        tokens.add(token);
    }

    public Token getAheadChar(int count) {
        return tokens.get(index + count);
    }

    public boolean hasNext() {
        return index < tokens.size();
    }

    public Token consume() {
        return tokens.get(index++);
    }
}
