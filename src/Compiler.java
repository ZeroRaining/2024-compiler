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
import midend.RemovePhi;
import midend.SSA.*;

import java.io.*;
import java.util.HashSet;

public class Compiler {
    public static void main(String[] args) throws IOException {
        // 解析命令行
        Arg arg = Arg.parse(args);
        BufferedInputStream source = new BufferedInputStream(arg.getSrcStream());
        BufferedWriter output = new BufferedWriter(arg.getAsmWriter());
        
        // 准备计时
        long startTime = 0;
        long optimizeStartTime = 0;
        long optimizeEndTime = 0;
        if (arg.toTime()) { startTime = System.currentTimeMillis(); }
        
        // 词法分析，得到 TokenList
        TokenList tokenList = Lexer.getInstance().lex(source);
        // 语法分析，得到 AST
        Ast ast = new Parser(tokenList).parseAst();
        // 生成 IR
        Program program = new Program(ast);
        HashSet<Function> functions = new HashSet<>(program.getFunctions().values());
        long time1 = System.currentTimeMillis();
        DFG.doDFG(functions);
        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);

        // 开启优化
        if (arg.toTime()) { optimizeStartTime = System.currentTimeMillis(); }
        if (arg.getOptLevel() == 1) {
            Mem2Reg.doMem2Reg(functions);
            DeadCodeRemove.doDeadCodeRemove(functions);
            GVN.doGVN(functions);
            OIS.doOIS(functions);
        }
        if (arg.toTime()) { optimizeEndTime = System.currentTimeMillis(); }
        RemovePhi.removePhi(functions);
        // 打印 IR
        if (arg.toPrintIR()) {
            BufferedWriter irWriter = new BufferedWriter(arg.getIrWriter());
            program.printIR(irWriter);
            irWriter.close();
        }

        // 运行后端
        if (!arg.toSkipBackEnd()) {
            AsmModule asmModule = new IrParser(program).parse();
            RegAlloc alloc = RegAlloc.getInstance();
            alloc.run(asmModule);
            BackendPrinter backendPrinter = new BackendPrinter(asmModule, true, output);
            backendPrinter.printBackend();
        }
        
        // 计算运行时间
        if (arg.toTime()) {
            long endTime = System.currentTimeMillis();
            long runTime = endTime - startTime;
            long optimizingTime = optimizeEndTime - optimizeStartTime;
            
            System.out.println("runTime: " + runTime + "ms");
            System.out.println("optimizingTime: " + optimizingTime + "ms");
        }
        
        
        // IR生成测试
//        IRTest();

        //后端代码生成测试
       // CodeGenTest();

        //寄存器分配测试
//        RegAllocTest();
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
        //Mem2Reg.doMem2Reg(functions);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.ll"));
        program.printIR(writer);
        writer.close();

        //代码生成测试
        AsmModule asmModule = new IrParser(program).parse();
        BackendPrinter backendPrinter = new BackendPrinter(asmModule,false);
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
        BackendPrinter backendPrinter = new BackendPrinter(asmModule,true);
        backendPrinter.printBackend();
    }
}

//
