import arg.Arg;
import frontend.lexer.Token;
import frontend.lexer.Lexer;
import frontend.lexer.TokenList;
import frontend.syntax.Ast;
import frontend.syntax.Parser;

import java.io.*;

public class Compiler {
    public static void main(String[] args) throws IOException {
        //解析命令行
        //Arg arg = Arg.parse(args);
        //BufferedInputStream source = new BufferedInputStream(arg.srcStream);

        //词法分析，得到TokenList
        //TokenList tokenList = Lexer.getInstance().lex(source);

        //语法分析，得到AST
        //Ast ast = new Parser(tokenList).parseAst();

        //词法分析测试
        //LexerTest();

        //语法分析测试
        ParserTest();
    }

    public static void LexerTest() throws IOException {
        //词法分析测试
        FileInputStream in = new FileInputStream("lexerTest.txt");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        while (tokenList.hasNext()) {
            Token token = tokenList.getChar();
            System.out.println(token.getType() + " " + token.getContent());
        }
    }

    public static void ParserTest() throws IOException {
        //语法分析测试
        FileInputStream in = new FileInputStream("parserTest.txt");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        System.out.println(ast);
    }
}
//
