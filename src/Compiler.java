import arg.Arg;
import backend.BackendPrinter;
import backend.IrParser;
import backend.RegAlloc;
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
import java.util.HashSet;

public class Compiler {
    public static void main(String[] args) throws IOException {
        //解析命令行
        Arg arg = Arg.parse(args);
        BufferedInputStream source = new BufferedInputStream(arg.getSrcStream());
        // asm 输出流还没有哦

        //词法分析，得到TokenList
        TokenList tokenList = Lexer.getInstance().lex(source);
        //语法分析，得到AST
        Ast ast = new Parser(tokenList).parseAst();
        //IR生成
        Program program = new Program(ast);
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        DFG.doDFG(functions);

        // 开启优化
        if (arg.getOptLevel() == 1) {
            Mem2Reg.doMem2Reg(functions);
        }

        // 打印 IR
        if (arg.toPrintIR()) {
            BufferedWriter irWriter = new BufferedWriter(arg.getIrWriter());
            program.printIR(irWriter);
            irWriter.close();
        }

        // IR生成测试
        //IRTest();

        //后端代码生成测试
        //CodeGenTest();

        //寄存器分配测试
        //RegAllocTest();
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
        DFG.doDFG(functions);
        Mem2Reg.doMem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
        writer.close();
    }

    public static void CodeGenTest() throws IOException {
        //语法分析测试
        FileInputStream in = new FileInputStream("in.sy");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        Program program = new Program(ast);
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        DFG.doDFG(functions);
        Mem2Reg.doMem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
        writer.close();

        //代码生成测试
        AsmModule asmModule = new IrParser(program).parse();
        BackendPrinter backendPrinter = new BackendPrinter(asmModule);
        backendPrinter.printBackend();
    }

    public static void RegAllocTest() throws IOException {
        //
        FileInputStream in = new FileInputStream("in.sy");
        BufferedInputStream source = new BufferedInputStream(in);
        TokenList tokenList = Lexer.getInstance().lex(source);
        Ast ast = new Parser(tokenList).parseAst();
        Program program = new Program(ast);
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        DFG.doDFG(functions);
        //Mem2Reg.doMem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
        writer.close();
        AsmModule asmModule = new IrParser(program).parse();
        RegAlloc alloc = RegAlloc.getInstance();
        alloc.run(asmModule);
        BackendPrinter backendPrinter = new BackendPrinter(asmModule);
        backendPrinter.printBackend();
    }
}

//
