import arg.Arg;
import frontend.lexer.Token;
import frontend.lexer.Lexer;
import frontend.lexer.TokenList;

import java.io.*;

public class Compiler {
    public static void main(String[] args) throws IOException {
        //解析命令行
        Arg arg = Arg.parse(args);
        BufferedInputStream source = new BufferedInputStream(arg.srcStream);

        //词法分析，得到TokenList
        TokenList tokenList = Lexer.getInstance().lex(source);

        //词法分析测试
        LexerTest();
    }

    public static void LexerTest() throws IOException {
        //词法分析测试
        FileInputStream in = new FileInputStream("lexerTest.txt");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        while (tokenList.hasNext()) {
            Token token = tokenList.consume();
            System.out.println(token.getType() + " " + token.getContent());
        }
    }
}
//
