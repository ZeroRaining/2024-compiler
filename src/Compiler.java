import backend.BackendPrinter;
import backend.IrParser;
import backend.itemStructure.AsmModule;
import frontend.ir.structure.Function;
import frontend.ir.structure.Program;
import frontend.lexer.Token;
import frontend.lexer.Lexer;
import frontend.lexer.TokenList;
import frontend.syntax.Ast;
import frontend.syntax.Parser;
import midend.SSA.DFG;
import midend.SSA.Mem2Reg;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class Compiler {
    public static void main(String[] args) throws IOException {
        //解析命令行
        //Arg arg = Arg.parse(args);
        //BufferedInputStream source = new BufferedInputStream(arg.srcStream);

        //词法分析，得到TokenList
        //TokenList tokenList = Lexer.getInstance().lex(source);

        //语法分析，得到AST
        //Ast ast = new Parser(tokenList).parseAst();

        //IR生成
        //Program program = new Program(ast);

        //词法分析测试
        //LexerTest();

        //语法分析测试
        //ParserTest();

        //IR生成测试
        //IRTest();

        //irParser测试
        IRParserTest();
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
        FileInputStream in = new FileInputStream("in.sy");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        System.out.println(ast);
    }

    public static void IRTest() throws IOException {
        //语法分析测试
        FileInputStream in = new FileInputStream("in.sy");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        Program program = new Program(ast);
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        DFG dfg = new DFG(functions);
//        Mem2Reg mem2Reg = new Mem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
        writer.close();
    }

    public static void IRParserTest() throws IOException {
        //语法分析测试
        FileInputStream in = new FileInputStream("in.sy");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        Program program = new Program(ast);
//        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
//        DFG dfg = new DFG(functions);
//        Mem2Reg mem2Reg = new Mem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
//        writer.close();

        //IRParser测试
        AsmModule asmModule = new IrParser(program).parse();
        new BackendPrinter(asmModule).printBackend();
    }
}

